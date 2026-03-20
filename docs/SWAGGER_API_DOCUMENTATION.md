# Swagger/OpenAPI Documentation

## Accessing the API Documentation

### Swagger UI
**URL:** `http://localhost:8080/swagger-ui.html`

Interactive API documentation where you can:
- View all API endpoints
- Read detailed descriptions and parameters
- Try out endpoints with test requests
- See response examples and schemas

### OpenAPI JSON
**URL:** `http://localhost:8080/v3/api-docs`

Raw OpenAPI specification in JSON format for integration with tools and code generators.

### ReDoc (Alternative UI)
**URL:** `http://localhost:8080/v3/api-docs.yaml` → Use with ReDoc

---

## API Overview

### Base URL
- **Development:** `http://localhost:8080`
- **Production:** `https://api.portfoliomanager.com`

### Authentication
All endpoints (except `/auth/login` and `/auth/register`) require JWT Bearer token authentication.

**Header Format:**
```
Authorization: Bearer <access_token>
```

**Token Lifespan:**
- Access Token: 30 minutes
- Refresh Token: 7 days
- Use `/api/auth/refresh-token` to get a new access token before expiration

---

## API Endpoints

### Authentication Endpoints (`/api/auth`)

#### 1. Register User
```
POST /api/auth/register
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "pan": "ABCDE1234F",
  "firstName": "John",
  "lastName": "Doe",
  "password": "secure123"
}

Response (201):
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "pan": "ABCDE1234F",
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 1800,
  "tokenType": "Bearer"
}
```

#### 2. Login
```
POST /api/auth/login
Content-Type: application/json

Request Body:
{
  "email": "user@example.com",
  "password": "secure123"
}

Response (200):
{
  "id": 1,
  "email": "user@example.com",
  "name": "John Doe",
  "pan": "ABCDE1234F",
  "token": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 1800,
  "tokenType": "Bearer"
}
```

#### 3. Refresh Access Token
```
POST /api/auth/refresh-token
Content-Type: application/json
Authorization: Bearer <refresh_token>

Request Body:
{
  "refreshToken": "eyJhbGc..."
}

Response (200):
{
  "token": "eyJhbGc...",
  "expiresIn": 1800,
  "tokenType": "Bearer"
}
```

#### 4. Get Current User Profile
```
GET /api/auth/me?userId=1
Authorization: Bearer <access_token>

Response (200):
{
  "email": "user@example.com",
  "name": "John Doe",
  "pan": "ABCDE1234F",
  "phone": "+1-234-567-8900",
  "address": "123 Main St",
  "kycStatus": "VERIFIED",
  "createdAt": "2026-02-14T10:30:00",
  "updatedAt": "2026-02-14T10:30:00"
}
```

#### 5. Update User Profile
```
PUT /api/auth/profile?userId=1
Content-Type: application/json
Authorization: Bearer <access_token>

Request Body:
{
  "phone": "+1-987-654-3210",
  "address": "456 Oak Ave",
  "kycStatus": "VERIFIED"
}

Response (200):
Updated user object
```

---

### User Management Endpoints (`/api/users`)

#### 1. Get Portfolio Snapshot
```
GET /api/users/{email}/snapshot
Authorization: Bearer <access_token>

Response (200):
{
  "totalInvested": 500000,
  "currentValue": 650000,
  "totalGain": 150000,
  "gainPercentage": 30.0,
  "portfolioValue": {
    "equity": 350000,
    "debt": 200000,
    "balanced": 100000
  }
}
```

#### 2. List All Users (Paginated)
```
GET /api/users?page=0&size=20
Authorization: Bearer <access_token>

Response (200):
{
  "content": [
    {
      "id": 1,
      "email": "user@example.com",
      "name": "John Doe",
      "pan": "ABCDE1234F"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0
}
```

#### 3. Upload Files (Transaction/Valuation/ZIP)
```
POST /api/users/upload-files
Content-Type: multipart/form-data
Authorization: Bearer <access_token>

Parameters:
- email (required): User's email
- rtaName (required): Fund house name (CAMS, Kfintech, etc.)
- transactionFile (optional): Transaction CSV file
- valuationFile (optional): Valuation CSV file
- zipFile (optional): ZIP archive containing files
- zipPassword (optional): Password for encrypted ZIP
- importId (optional): Existing import ID to continue

Response (202):
{
  "importId": 123,
  "status": "PROCESSING",
  "uploadMode": "ZIP",
  "filesProcessed": 2,
  "message": "ZIP file uploaded and queued for processing"
}
```

**Requirements:**
- Email in files must match user's registered email (case-insensitive)
- At least one file must be provided
- ZIP files are supported with optional password protection

#### 4. Upload Transaction File
```
POST /api/users/upload-transactions
Content-Type: multipart/form-data
Authorization: Bearer <access_token>

Parameters:
- email (required): User's email
- rtaName (required): Fund house name
- file (required): Transaction file
- isValuationFile (required): true if valuation, false if transaction
- importId (optional): Existing import ID

Response (202):
{
  "importId": 123,
  "status": "PROCESSING",
  "message": "File uploaded and queued for processing"
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "Invalid data: Email in file (contact@test.com) does not match user email (user@example.com)",
  "timestamp": "2026-02-14T10:30:00",
  "status": 400
}
```

### 401 Unauthorized
```json
{
  "error": "Unauthorized - Invalid or expired token",
  "timestamp": "2026-02-14T10:30:00",
  "status": 401
}
```

### 404 Not Found
```json
{
  "error": "User not found with email: user@example.com",
  "timestamp": "2026-02-14T10:30:00",
  "status": 404
}
```

### 500 Internal Server Error
```json
{
  "error": "Failed to process files",
  "timestamp": "2026-02-14T10:30:00",
  "status": 500
}
```

---

## Common Use Cases

### 1. User Registration and Login
```
1. POST /api/auth/register
   → Get access token and refresh token
2. Store tokens in localStorage
3. Use access token for all subsequent requests
```

### 2. Maintain Authentication
```
1. Access token expires after 30 minutes
2. Monitor token expiration time
3. Before expiration, POST /api/auth/refresh-token
   → Get new access token
4. Continue without interruption
```

### 3. Upload Transaction Files
```
1. POST /api/users/upload-files
   - email: User's registered email
   - rtaName: Fund house name
   - transactionFile: CSV with transactions (must include user email)
2. System validates email in file matches user
3. File is queued for processing
4. Monitor via importId status
```

### 4. Upload Password-Protected ZIP
```
1. POST /api/users/upload-files
   - zipFile: Password-protected ZIP
   - zipPassword: ZIP password
   - email: User's email
   - rtaName: Fund house name
2. System extracts ZIP using password
3. Validates files inside ZIP
4. Processes transaction and valuation files
```

---

## Schema Definitions

### AuthResponse
```json
{
  "id": 1,
  "email": "string",
  "name": "string",
  "pan": "string",
  "token": "string (JWT)",
  "refreshToken": "string (JWT)",
  "expiresIn": 1800,
  "tokenType": "Bearer"
}
```

### PortfolioSnapshot
```json
{
  "totalInvested": 500000,
  "currentValue": 650000,
  "totalGain": 150000,
  "gainPercentage": 30.0,
  "portfolioValue": {
    "equity": 350000,
    "debt": 200000,
    "balanced": 100000
  }
}
```

### UploadResponse
```json
{
  "importId": 123,
  "status": "PROCESSING",
  "uploadMode": "ZIP",
  "filesProcessed": 2,
  "message": "string"
}
```

---

## Security Notes

### API Key / Token Handling
1. **Never commit tokens** to version control
2. **Use HTTPS** in production
3. **Store tokens securely** (localStorage or secure cookies)
4. **Include Bearer token** in Authorization header
5. **Refresh tokens** before expiration

### Validation
- Email validation (RFC 5322)
- PAN format validation (10 alphanumeric characters)
- File size limits enforced
- ZIP password support for encrypted archives

### Data Protection
- Email validation prevents unauthorized uploads
- Case-insensitive email comparison
- Clear error messages without information leakage

---

## Testing the API

### Using curl
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "pan": "ABCDE1234F",
    "firstName": "Test",
    "lastName": "User",
    "password": "pass123"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "password": "pass123"}'

# Use token
curl -X GET http://localhost:8080/api/auth/me?userId=1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Using Postman
1. Import OpenAPI spec from `http://localhost:8080/v3/api-docs`
2. Create environment variable for `token`
3. Copy token from login response to environment
4. Use `{{token}}` in Authorization headers

### Using Swagger UI
1. Open `http://localhost:8080/swagger-ui.html`
2. Click "Authorize" button
3. Enter Bearer token
4. Try endpoints directly

---

## Troubleshooting

### 401 Unauthorized
- Check token is included in Authorization header
- Verify token format: `Bearer <token>`
- Token may be expired, refresh it

### 400 Bad Request (Email Mismatch)
- Email in CSV file must match registered user email
- Check file contains correct email column
- Email comparison is case-insensitive

### 404 Not Found
- User with email may not exist
- Register user first via `/api/auth/register`

### 202 Accepted (Processing)
- File upload was accepted
- Processing happens asynchronously
- Use importId to monitor status

---

## Documentation Files

- **Swagger Config:** `src/main/java/com/sudheer/portfoliotracker/config/SwaggerConfig.java`
- **API Docs:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

---

## Related Documentation

- Token Auth: `docs/TOKEN_AUTH_QUICK_REFERENCE.md`
- Upload Validation: `docs/TOKEN_AUTH_EMAIL_VALIDATION_IMPLEMENTATION.md`
- Test Coverage: `docs/AMFI_SYNC_CONTROLLER_TEST_COVERAGE.md`

