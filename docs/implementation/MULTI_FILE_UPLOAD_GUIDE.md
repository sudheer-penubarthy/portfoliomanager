# Multi-File Upload Feature Documentation

## Overview

The Portfolio Manager now supports uploading transaction and valuation data in multiple formats:

1. **Individual Files** - Upload transaction and valuation files separately
2. **ZIP Archive** - Upload both files in a single ZIP file

This enables flexible data import workflows while maintaining data consistency and avoiding duplicates.

## Supported File Types

### File Naming Conventions

**Transaction Files:**
- **Format:** Alphanumeric string (letters and/or numbers only)
- **Examples:** `ABC123.txt`, `Kfintech.csv`, `CAMS2024.txt`
- **Content:** Detailed transaction history with columns like scheme_code, transaction_type, units, amount, date

**Valuation Files:**
- **Format:** Named `CurrentValuation` + transaction filename
- **Examples:** 
  - If transaction file is `ABC123.txt`, valuation file should be `CurrentValuationABC123.txt`
  - If transaction file is `CAMS2024.csv`, valuation file should be `CurrentValuationCAMS2024.csv`
- **Content:** Snapshot of current holdings with scheme codes, units, and current values

## API Endpoints

### 1. Upload Individual Files

**Endpoint:** `POST /api/users/upload-files`

**Content-Type:** `multipart/form-data`

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | String | Yes | User's email address (must exist in system) |
| `rtaName` | String | Yes | RTA/Custodian name (e.g., "CAMS", "Kfintech") |
| `transactionFile` | File | No* | Transaction file (alphanumeric name) |
| `valuationFile` | File | No* | Valuation snapshot file |
| `importId` | Long | No | Optional existing import ID to continue |

*At least one file must be provided (either transactionFile or valuationFile or both)

**Example (curl):**

```bash
curl -X POST http://localhost:8080/api/users/upload-files \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "transactionFile=@ABC123.txt" \
  -F "valuationFile=@CurrentValuation.txt"
```

**Example (Python):**

```python
import requests

files = {
    'transactionFile': open('ABC123.txt', 'rb'),
    'valuationFile': open('CurrentValuation.txt', 'rb')
}

data = {
    'email': 'user@example.com',
    'rtaName': 'CAMS'
}

response = requests.post(
    'http://localhost:8080/api/users/upload-files',
    files=files,
    data=data
)

print(response.json())
```

### 2. Upload ZIP Archive

**Endpoint:** `POST /api/users/upload-files`

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | String | Yes | User's email address |
| `rtaName` | String | Yes | RTA/Custodian name |
| `zipFile` | File | Yes* | ZIP archive containing files |
| `importId` | Long | No | Optional existing import ID |

*Use this OR provide individual files

**ZIP Archive Structure:**

```
portfolio_data.zip
├── ABC123.txt                  (transaction file)
└── CurrentValuationABC123.txt  (valuation file with naming convention)
```

**Example (curl):**

```bash
# Create ZIP file
zip portfolio_data.zip ABC123.txt CurrentValuation.txt

# Upload
curl -X POST http://localhost:8080/api/users/upload-files \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "zipFile=@portfolio_data.zip"
```

**Example (Python):**

```python
import requests
import zipfile
import os

# Create ZIP file
with zipfile.ZipFile('portfolio_data.zip', 'w') as zf:
    zf.write('ABC123.txt')
    zf.write('CurrentValuation.txt')

# Upload
files = {'zipFile': open('portfolio_data.zip', 'rb')}
data = {
    'email': 'user@example.com',
    'rtaName': 'CAMS'
}

response = requests.post(
    'http://localhost:8080/api/users/upload-files',
    files=files,
    data=data
)

print(response.json())

# Clean up
os.remove('portfolio_data.zip')
```

## Response Format

**Status:** 202 Accepted (file is queued for processing)

**Response Body:**

```json
{
  "importId": 123,
  "status": "PROCESSING",
  "uploadMode": "ZIP|INDIVIDUAL",
  "filesProcessed": 2,
  "message": "File(s) uploaded and queued for processing"
}
```

**Fields:**
- `importId`: Unique ID to track import progress
- `status`: Current status (PROCESSING, COMPLETED, or FAILED)
- `uploadMode`: How files were uploaded (ZIP or INDIVIDUAL)
- `filesProcessed`: Number of valid files extracted/uploaded
- `message`: Human-readable status message

## File Processing Logic

### Transaction File Processing

1. **Validation:**
   - File is checked for valid CSV/TSV format
   - Required columns are verified
   - Each row is parsed for scheme code, transaction type, units, amount, date

2. **Deduplication:**
   - Each transaction row includes a `sourceReference` (unique identifier)
   - System checks if this reference already exists for the user
   - Duplicate transactions are skipped (logged and counted in progress)

3. **Storage:**
   - Non-duplicate transactions are inserted into `user_transaction` table
   - Transactions are batched (1000 per batch) for performance

4. **Holdings Recomputation:**
   - After all transactions are loaded, holdings are recalculated
   - Holdings aggregation:
     - BUY transactions add to units and cost
     - SELL transactions subtract from units and cost
   - Zero-unit holdings are automatically removed

### Valuation File Processing

1. **Validation:**
   - File format is verified
   - Required columns checked (scheme code, units, current value)

2. **Upsert Logic:**
   - For each scheme code in the file:
     - Any existing holdings for that scheme are deleted
     - New/updated holdings are inserted
   - This ensures the snapshot reflects current state

3. **Atomic Operation:**
   - Valuation import is transactional
   - Either all schemes update or none (no partial updates)

## Duplicate Handling

### Transaction Deduplication

The system avoids duplicate transactions using the `sourceReference` field:

- Each transaction from the source file must have a unique `sourceReference`
- Common formats: `CAMS_12345_001`, `KFIN_ORDER_2024_001`, etc.
- If a transaction with the same reference exists, it's skipped:
  - Logged as skipped
  - Not inserted into database
  - Counted in import progress

**Benefits:**
- Safe re-uploads: uploading the same file twice won't create duplicates
- Incremental imports: can upload new transactions without worrying about old ones
- Audit trail: each transaction traceable to its source

### Valuation Deduplication

Valuations use a "last-update-wins" strategy:

- All existing holdings for processed schemes are deleted
- New holdings are inserted
- Result: snapshot always reflects the most recent upload

This is intentional for valuation data (point-in-time snapshots).

## Import Progress Tracking

The `importId` returned in the response can be used to track progress:

**Endpoint:** `GET /api/users/import/{importId}/progress`

**Response:**

```json
{
  "importId": 123,
  "status": "PROCESSING|COMPLETED|FAILED",
  "rowsProcessed": 1500,
  "rowsInserted": 1450,
  "rowsSkipped": 50,
  "errorMessage": null
}
```

## Error Handling

### File Validation Errors

**Invalid file format:**
```json
{
  "error": "Bad Request",
  "message": "Failed to process uploaded files: Invalid CSV format"
}
```

**No files provided:**
```json
{
  "error": "Bad Request",
  "message": "At least one file must be provided: transaction file, valuation file, or ZIP archive"
}
```

**ZIP with no valid files:**
```json
{
  "error": "Bad Request",
  "message": "ZIP archive must contain at least one valid file (transaction file or 'CurrentValuation' file)"
}
```

### User Validation Errors

**User not found:**
```json
{
  "error": "Not Found",
  "message": "User not found with email: user@example.com"
}
```

**Invalid email:**
```json
{
  "error": "Bad Request",
  "message": "Email must be valid"
}
```

## Password-Protected ZIP Support

**Status:** Currently not supported

**Note:** Standard Java ZIP API doesn't support password-protected archives. To enable this feature:

1. Add dependency:
   ```gradle
   implementation 'org.apache.commons:commons-compress:1.25.0'
   ```

2. Add encryption library:
   ```gradle
   implementation 'net.lingala.zip4j:zip4j:2.11.5'
   ```

3. Use `ZipHandlerService.extractFromPasswordProtectedZip(stream, password)`

**Endpoint for future implementation:**
```
POST /api/users/upload-zip-protected
Parameters:
  - email: user email
  - rtaName: RTA name
  - zipFile: password-protected ZIP
  - password: ZIP password
```

## Best Practices

### File Preparation

1. **Transaction Files:**
   - Use consistent naming (e.g., `CAMS.txt`, `Kfintech.txt`)
   - Include header row
   - One transaction per line
   - Ensure sourceReference is unique per transaction

2. **Valuation Files:**
   - Always name as `CurrentValuation.txt` or `CurrentValuation.csv`
   - Include all current holdings
   - Ensure scheme codes match transaction file schemes

3. **ZIP Archives:**
   - Keep files at root level (avoid nested folders)
   - Use descriptive names
   - Don't exceed 50MB per archive
   - Ensure files are valid before zipping

### Workflow

```
1. Prepare files locally
   ├── Validate format
   ├── Check for duplicates
   └── Name correctly

2. Upload
   ├── Single transaction file
   ├── OR single valuation file
   ├── OR both in ZIP
   └── Receive importId

3. Track progress (optional)
   └── Check import status

4. Verify results
   ├── Query snapshot endpoint
   ├── Check portfolio value
   └── Review transaction history

5. Repeat for next period
```

### Performance Considerations

- **Batch Size:** 1000 transactions per batch (configurable)
- **ZIP Processing:** Extracted to memory (suitable for files < 100MB)
- **Deduplication:** O(1) lookup using sourceReference index
- **Recomputation:** O(n) where n = number of transactions

For large imports (> 10,000 transactions), consider:
- Splitting into multiple smaller files
- Staggering uploads across different times
- Monitoring server logs for performance

## Testing

### Test Scenarios

1. **Individual transaction file upload**
2. **Individual valuation file upload**
3. **Both files separately**
4. **ZIP archive with both files**
5. **ZIP archive with only transactions**
6. **ZIP archive with only valuations**
7. **Duplicate transaction handling**
8. **Invalid file format handling**
9. **Missing files in ZIP**
10. **User not found handling**

### Sample Test Data

**Transaction File (ABC123.txt):**
```
sourceReference,txnDate,schemeCode,txnType,units,amount
CAMS_001_2024,2024-01-15,0P0000KJWA,BUY,100.50,10050.00
CAMS_002_2024,2024-02-20,0P0000KJWA,BUY,50.25,5025.00
CAMS_003_2024,2024-03-10,0P000088UP,BUY,200.00,20000.00
```

**Valuation File (CurrentValuationABC123.txt):**
```
schemeCode,units,currentValue
0P0000KJWA,150.75,22610.50
0P000088UP,200.00,26000.00
```

## Migration from Old Format

If migrating from single-file uploads:

```bash
# Old way:
curl -X POST http://localhost:8080/api/users/upload-transactions \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "file=@ABC123.txt" \
  -F "isValuationFile=false"

# New way (still works):
curl -X POST http://localhost:8080/api/users/upload-transactions \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "file=@ABC123.txt" \
  -F "isValuationFile=false"

# New multi-file way:
curl -X POST http://localhost:8080/api/users/upload-files \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "transactionFile=@ABC123.txt" \
  -F "valuationFile=@CurrentValuation.txt"
```

Both old and new endpoints are supported for backward compatibility.

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| "ZIP archive must contain at least one valid file" | No files matched naming convention | Check file names: transactions should be alphanumeric, valuations named "CurrentValuation" |
| "User not found" | Email doesn't exist in system | Create user first or use correct email |
| "Uploaded file cannot be empty" | File has 0 bytes | Check file content and try again |
| Files processed but no data updated | Possible duplicate transactions | Check sourceReference fields for duplicates |
| Valuation imports very slowly | Large file size or network latency | Try splitting into smaller ZIP files |

## Future Enhancements

1. **Password-protected ZIP support**
2. **CSV format validation/preview before import**
3. **Batch import scheduling**
4. **Import templates and presets**
5. **Transaction reconciliation reports**
6. **Automatic duplicate detection and merge**
7. **Import history and rollback capability**

---

For questions or issues, contact support or check the application logs for detailed error messages.

