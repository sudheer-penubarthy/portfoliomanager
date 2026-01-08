# Multi-File Upload API Quick Reference

## New Endpoint

### Upload Files (Individual or ZIP)
```
POST /api/users/upload-files
Content-Type: multipart/form-data
```

## Parameters

| Name | Type | Required | Description |
|------|------|----------|-------------|
| email | String | ✅ Yes | User's email (must exist) |
| rtaName | String | ✅ Yes | RTA/Custodian name |
| transactionFile | File | ⚠️ One of | Transaction file (alphanumeric name) |
| valuationFile | File | ⚠️ One of | Valuation file (named "CurrentValuation") |
| zipFile | File | ⚠️ One of | ZIP archive with both files |
| importId | Long | ❌ No | Optional existing import ID |

**Note:** Must provide at least one of: `transactionFile`, `valuationFile`, or `zipFile`

## File Naming Conventions

**Transaction Files:**
```
ABC123.txt              ✅ Valid
CAMS2024.csv           ✅ Valid
Kfintech.tsv           ✅ Valid
123456.txt             ✅ Valid
file-with-dashes.txt   ❌ Invalid (special chars)
CurrentValuation.txt   ❌ Invalid (reserved name)
```

**Valuation Files:**
```
CurrentValuationABC123.txt    ✅ Valid (matches ABC123.txt transaction)
CurrentValuationCAMS2024.csv  ✅ Valid (matches CAMS2024.csv transaction)
currentvaluationKFIN.json     ✅ Valid (case-insensitive, matches KFIN transaction)
CurrentValuation.txt          ❌ Invalid (missing transaction name part)
ValuationSnapshot.txt         ❌ Invalid (wrong name)
CV.txt                        ❌ Invalid (abbreviated)
```

**Note:** Valuation file name MUST include "CurrentValuation" + the transaction filename (without extension)

## Quick Examples

### cURL - Individual Files
```bash
curl -X POST http://localhost:8080/api/users/upload-files \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "transactionFile=@ABC123.txt" \
  -F "valuationFile=@CurrentValuation.txt"
```

### cURL - Transaction Only
```bash
curl -X POST http://localhost:8080/api/users/upload-files \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "transactionFile=@ABC123.txt"
```

### cURL - ZIP Archive
```bash
# Create ZIP
zip portfolio.zip ABC123.txt CurrentValuation.txt

# Upload
curl -X POST http://localhost:8080/api/users/upload-files \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "zipFile=@portfolio.zip"
```

### Python - Multiple Files
```python
import requests

files = {
    'transactionFile': open('ABC123.txt', 'rb'),
    'valuationFile': open('CurrentValuation.txt', 'rb')
}
data = {'email': 'user@example.com', 'rtaName': 'CAMS'}

resp = requests.post(
    'http://localhost:8080/api/users/upload-files',
    files=files,
    data=data
)
print(resp.json())
```

### Python - ZIP File
```python
import requests
import zipfile

# Create ZIP
with zipfile.ZipFile('portfolio.zip', 'w') as zf:
    zf.write('ABC123.txt')
    zf.write('CurrentValuation.txt')

# Upload
files = {'zipFile': open('portfolio.zip', 'rb')}
data = {'email': 'user@example.com', 'rtaName': 'CAMS'}

resp = requests.post(
    'http://localhost:8080/api/users/upload-files',
    files=files,
    data=data
)
print(resp.json())
```

## Response Format

### Success (202 Accepted)
```json
{
  "importId": 123,
  "status": "PROCESSING",
  "uploadMode": "ZIP",
  "filesProcessed": 2,
  "message": "ZIP file uploaded and queued for processing"
}
```

### Error (400 Bad Request)
```json
{
  "error": "Bad Request",
  "message": "At least one file must be provided: ...",
  "status": 400
}
```

### Error (404 Not Found)
```json
{
  "error": "Not Found",
  "message": "User not found with email: user@example.com",
  "status": 404
}
```

## Response Fields

| Field | Type | Description |
|-------|------|-------------|
| importId | Long | ID for tracking import progress |
| status | String | Current status: PROCESSING, COMPLETED, FAILED |
| uploadMode | String | How files were uploaded: ZIP or INDIVIDUAL |
| filesProcessed | Int | Number of valid files extracted |
| message | String | Human-readable status message |
| error | String | Error type (on failure) |

## HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 202 | Accepted | Files queued for processing ✅ |
| 400 | Bad Request | Invalid input, missing files ❌ |
| 404 | Not Found | User email doesn't exist ❌ |
| 500 | Server Error | Processing failure ❌ |

## Processing Logic

### Transaction File
```
1. Validate format
2. Deduplicate (check sourceReference)
3. Batch insert (1000 per batch)
4. Recompute holdings
5. Mark complete
```

### Valuation File
```
1. Validate format
2. Delete existing holdings for schemes
3. Insert new holdings
4. Mark complete
```

### ZIP File
```
1. Extract archive
2. Classify files
3. Process each file sequentially
4. Mark import complete
```

## Deduplication

**Transactions:** Skip if `sourceReference` exists for user
```
Example sourceReference:
  CAMS_ORDER_2024_001
  KFIN_TXN_20240115_123
  USER_ABC_2024_JAN_01
```

**Valuations:** Delete old, insert new (last-update-wins)

## Common Errors

| Error | Cause | Fix |
|-------|-------|-----|
| "User not found" | Email doesn't exist | Create user first |
| "At least one file" | No files provided | Provide file or ZIP |
| "ZIP archive must contain" | Invalid filenames | Check naming convention |
| "Email must be valid" | Invalid email format | Use valid email |
| "File is required" | Empty file | Check file has content |

## Best Practices

✅ **DO:**
- Use consistent transaction file names
- Name valuation files exactly as "CurrentValuation"
- Include unique sourceReference per transaction
- Test with small files first
- Keep files < 50MB

❌ **DON'T:**
- Use special characters in transaction filenames
- Nest files in ZIP directories
- Upload duplicate transactions (same sourceReference)
- Mix file formats in single ZIP
- Upload invalid CSV format

## Workflow Example

```bash
# 1. Prepare files
cat > ABC123.txt << EOF
sourceReference,txnDate,schemeCode,txnType,units,amount
CAMS_001,2024-01-15,0P000088UP,BUY,100,10000
CAMS_002,2024-02-20,0P000088UP,BUY,50,5000
EOF

cat > CurrentValuationABC123.txt << EOF
schemeCode,units,currentValue
0P000088UP,150,22500
EOF

# 2. Create ZIP
zip portfolio.zip ABC123.txt CurrentValuationABC123.txt

# 3. Upload
curl -X POST http://localhost:8080/api/users/upload-files \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "zipFile=@portfolio.zip"

# 4. Response
{
  "importId": 456,
  "status": "PROCESSING",
  "uploadMode": "ZIP",
  "filesProcessed": 2
}

# 5. Track progress (future endpoint)
curl http://localhost:8080/api/users/import/456/progress
```

## Compatibility

- ✅ Backward compatible with old `/api/users/upload-transactions` endpoint
- ✅ Spring Boot 3.2+
- ✅ Java 17+
- ✅ All recent browsers

## Support

For detailed documentation: See `MULTI_FILE_UPLOAD_GUIDE.md`

For troubleshooting: See GUIDE section "Troubleshooting"

For implementation details: See `MULTI_FILE_UPLOAD_IMPLEMENTATION_SUMMARY.md`

