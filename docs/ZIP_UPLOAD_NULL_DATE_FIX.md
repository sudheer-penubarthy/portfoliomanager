# ZIP Upload - Null Transaction Date Fix

## Problem

When uploading ZIP files containing transaction data, the application was throwing a `NullPointerException`:
```
Caused by: java.lang.NullPointerException: Cannot invoke "java.time.LocalDate.getYear()" because "date" is null
```

This error occurred during batch insertion of user transactions into the database.

## Root Cause

The issue was in the **CamsStreamParser.streamTransactions()** method:

1. The database schema defines `txn_date` as `DATE NOT NULL` - this column cannot accept null values
2. The parser's `parseDateLenient()` method attempts to parse transaction dates from the input file
3. If the date format doesn't match any of the supported formats, it returns `null`
4. The code was still building and inserting transactions with `null` txnDate values
5. When the batch insert tried to insert these null values, the database constraint violation occurred

### Problematic Code (Before)
```java
LocalDate txnDate = parseDateLenient(tradeDate);  // Could return null
// ... no validation ...
UserTransaction txn = UserTransaction.builder()
    .txnDate(txnDate)  // Null value set!
    // ... other fields ...
    .build();
consumer.accept(txn);  // Passed to batch insert
```

## Solution

Added validation to skip transactions with null `txnDate` since the date is mandatory:

### Fixed Code (After)
```java
LocalDate txnDate = parseDateLenient(tradeDate);
BigDecimal units = parseBigDecimalLenient(unitsStr);
BigDecimal amount = parseBigDecimalLenient(amountStr);
BigDecimal price = parseBigDecimalLenient(priceStr);
String txnType = mapCamsTxnType(transactionType);

// Validate mandatory fields: txnDate is required and cannot be null
if (txnDate == null) {
    log.warn("Skipping transaction at line {}: txn_date is mandatory but could not be parsed from: {}", lineno, tradeDate);
    continue;
}

// Validate units and amount exist
if (units == null || amount == null) {
    log.warn("Skipping transaction at line {}: units or amount is missing", lineno);
    continue;
}

// ... rest of processing ...
UserTransaction txn = UserTransaction.builder()
    .txnDate(txnDate)  // Now guaranteed non-null
    .units(units)      // Now guaranteed non-null
    .amount(amount)    // Now guaranteed non-null
    // ... other fields ...
    .build();
consumer.accept(txn);
```

## Changes Made

**File: `src/main/java/com/example/portfoliotracker/service/CamsStreamParser.java`**

### Key Improvements:

1. **Mandatory Field Validation**: Added check to skip transactions where `txnDate` cannot be parsed
2. **Units and Amount Validation**: Also validate that units and amount are present before processing
3. **Better Error Logging**: Log warnings when transactions are skipped with details about why
4. **Removed Null Coalescing**: Changed from `units == null ? BigDecimal.ZERO : units` to just using `units` directly since we now validate it's non-null

## Impact

- **Valid Transactions**: Only transactions with valid, parseable dates will be inserted
- **Invalid Transactions**: Transactions with unparseable dates are now skipped with detailed logging
- **Database Integrity**: Ensures `NOT NULL` database constraints are respected
- **Error Messages**: Users see clear warnings in logs about which lines were skipped and why

## Supported Date Formats

The parser supports the following date formats (as defined in `DateFormat` enum):
- `DD_MM_YYYY` (e.g., 08-12-2025)
- `DD_SLASH_MM_SLASH_YYYY` (e.g., 08/12/2025)
- `DD_MMM_YYYY` (e.g., 08-Dec-2025, 08-OCT-2025)
- `DD_MMMM_YYYY` (e.g., 08-December-2025)
- `YYYY_MM_DD` (e.g., 2025-12-08)
- `YYYY_SLASH_MM_SLASH_DD` (e.g., 2025/12/08)
- ISO standard format

If the date cannot be parsed with any of these formats, the transaction is skipped.

## Testing

To test this fix:

1. Create a CSV file with one transaction that has an invalid/unparseable date
2. Create a ZIP file containing this CSV
3. Upload the ZIP file via `/api/users/upload-files` endpoint
4. Check the application logs - you should see:
   ```
   WARN ... Skipping transaction at line X: txn_date is mandatory but could not be parsed from: [unparseable-date]
   ```
5. Only transactions with valid dates will be processed

## Recommendations

For future improvements:
1. Consider adding a column to track skipped rows in the AmfiImport table
2. Provide an API endpoint to retrieve skipped transaction details
3. Add validation in file upload response to indicate how many rows were skipped
4. Consider accepting a date format hint from the user when uploading files

## Related Files

- `src/main/java/com/example/portfoliotracker/service/CamsStreamParser.java` - Transaction parser
- `src/main/java/com/example/portfoliotracker/entity/UserTransaction.java` - Transaction entity
- `src/main/resources/db/migration/V5__create_user_tables.sql` - Database schema
- `src/main/java/com/example/portfoliotracker/enums/DateFormat.java` - Supported date formats

