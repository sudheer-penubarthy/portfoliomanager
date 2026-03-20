# Quick Reference: Token Auth & Email Validation

## Quick Start

### For Developers

#### Backend Setup
1. Add JWT_SECRET to environment variables (min 32 chars)
2. Start Spring Boot application
3. Server automatically validates tokens and emails

#### Frontend Setup
1. Users log in via `/api/auth/login`
2. AuthService stores tokens and starts monitoring
3. All API calls automatically include token via interceptor
4. Token automatically refreshes when about to expire

### For Users

1. **Login:** Enter email + password
2. **Upload Files:** 
   - Navigate to Dashboard → Upload
   - Select RTA/Fund House
   - Upload file(s) with your email in them
   - System validates email matches your account
3. **Automatic Refresh:** Token refreshes automatically - no action needed

---

## API Reference

### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

# Response
{
  "id": 1,
  "email": "user@example.com",
  "name": "User Name",
  "pan": "ABCDE1234F",
  "token": "eyJhbGc...",           # Access token (30 min)
  "refreshToken": "eyJhbGc...",   # Refresh token (7 days)
  "expiresIn": 1800,              # Seconds
  "tokenType": "Bearer"
}
```

### Refresh Token
```bash
POST /api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}

# Response
{
  "token": "eyJhbGc...",
  "expiresIn": 1800,
  "tokenType": "Bearer"
}
```

### Upload Files
```bash
POST /api/users/upload-files
Content-Type: multipart/form-data
Authorization: Bearer <access_token>

Parameters:
- email: user@example.com (required)
- rtaName: CAMS (required)
- transactionFile: file (optional)
- valuationFile: file (optional)
- zipFile: file (optional)
- zipPassword: password (optional, if ZIP encrypted)

# Response
{
  "importId": 1,
  "status": "PROCESSING",
  "uploadMode": "ZIP",
  "filesProcessed": 2,
  "message": "ZIP file uploaded and queued for processing"
}
```

---

## Token Behavior

### Access Token (30 minutes)
- Used for all API requests
- Auto-refreshed 5 minutes before expiration
- No user action required

### Refresh Token (7 days)
- Used to obtain new access token
- Stored securely in localStorage
- Only sent to `/api/auth/refresh-token` endpoint

### Auto-Refresh Timeline
```
Login at 10:00 AM
├─ Access token expires at 10:30 AM
├─ Auto-refresh triggered at 10:25 AM (5 min before)
├─ New access token generated
└─ User continues uninterrupted

After 7 days:
├─ Refresh token expires
├─ Auto-logout triggered
└─ User must login again
```

---

## Email Validation Rules

### Rule 1: Email Required
❌ Missing email → Upload fails  
✅ Email provided → Validation continues

### Rule 2: Valid Email Format
❌ invalid@email → Upload fails  
✅ user@example.com → Validation continues

### Rule 3: Email In File
❌ File has: contact@test.com  
   But user is: user@example.com → Upload fails  
   Error: "Invalid data: Email in file (contact@test.com) does not match user email (user@example.com)"

✅ File has: user@example.com  
   And user is: user@example.com → Upload succeeds

### Email Format Supported
- Extracted from CSV headers or first 10 lines
- Case-insensitive comparison
- Handles various delimiters: comma, tab, pipe
- Examples: 
  - `email,contact@example.com,amount`
  - `investor@example.com | 1000 units`
  - `contact@example.com	$5000`

---

## Common Errors & Solutions

### 1. "No user logged in"
**Cause:** Tried to access protected endpoint without login  
**Solution:** Call `/api/auth/login` first

### 2. "Invalid email or password"
**Cause:** Wrong credentials  
**Solution:** Verify email and password

### 3. "Email in file does not match user email"
**Cause:** File contains different email  
**Solution:** 
- Check file contents
- Ensure email matches logged-in user
- File must include your registered email

### 4. "Refresh token has expired. Please login again"
**Cause:** Refresh token older than 7 days  
**Solution:** Login again to get new tokens

### 5. "At least one file must be provided"
**Cause:** No file selected  
**Solution:** Upload transaction, valuation, or ZIP file

### 6. "Email cannot be blank"
**Cause:** Email field empty  
**Solution:** Provide valid email address

---

## File Upload Examples

### Single Transaction File
```
File: transactions.csv
Content:
email,scheme,units,amount
user@example.com,0P000088UP,100,10000
user@example.com,0P000089UP,50,5000
```

### Single Valuation File
```
File: CurrentValuation.csv
Content:
investor_email,fund,units,value
user@example.com,SBI Blue Chip,500,250000
user@example.com,HDFC Top 100,200,100000
```

### ZIP Archive
```
mydata.zip
├── transactions.csv (contains: user@example.com)
└── CurrentValuation.csv (contains: user@example.com)

Upload with password if encrypted:
- ZIP file: mydata.zip
- ZIP Password: mypassword123
```

---

## Configuration

### Environment Variables (Backend)

```bash
# JWT
JWT_SECRET=use-strong-32-character-secret-key-here
JWT_ACCESS_EXPIRATION=1800000      # 30 min in ms
JWT_REFRESH_EXPIRATION=604800000   # 7 days in ms

# Database
DB_URL=jdbc:mysql://localhost:3306/portfoliomanager
DB_USER=devuser
DB_PASS=devpass
```

### Local Development
```bash
# Default secret (dev only)
jwt.secret=dev-secret-key-minimum-32-characters

# File: src/main/resources/application.yml
jwt:
  secret: ${JWT_SECRET:dev-secret-key-minimum-32-characters}
  access-token-expiration: 1800000
  refresh-token-expiration: 604800000
```

---

## Testing

### Test Token Refresh
```javascript
// Browser console
const token = localStorage.getItem('token');
const refreshToken = localStorage.getItem('refreshToken');
const expiresIn = localStorage.getItem('expiresIn');

console.log('Access Token:', token?.substring(0, 20) + '...');
console.log('Expires In:', expiresIn, 'seconds');
```

### Test Email Extraction
```java
// Backend
FileMetadataExtractor extractor = new FileMetadataExtractor();
String email = extractor.extractEmailFromCsv(fileBytes);
String pan = extractor.extractPanFromCsv(fileBytes);

System.out.println("Extracted Email: " + email);
System.out.println("Extracted PAN: " + pan);
```

### Mock Test Upload
```bash
curl -X POST http://localhost:8080/api/users/upload-files \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "email=user@example.com" \
  -F "rtaName=CAMS" \
  -F "transactionFile=@transactions.csv"
```

---

## Key Files

| File | Purpose |
|------|---------|
| `JwtTokenService.java` | Token generation & validation |
| `FileMetadataExtractor.java` | Email/PAN extraction from files |
| `AuthController.java` | Login, register, refresh endpoints |
| `UserController.java` | Upload with email validation |
| `auth.service.ts` | Frontend token management |
| `auth.interceptor.ts` | Auto token injection & refresh |
| `upload-transactions.component.*` | Upload UI |

---

## Support & Debugging

### Enable Debug Logging
```yaml
# application.yml
logging:
  level:
    com.sudheer.portfoliotracker: DEBUG
    io.jsonwebtoken: DEBUG
```

### Check Token Contents
```bash
# Decode JWT (online tool)
https://jwt.io/

# Paste token to see claims:
# - userId
# - email
# - tokenType
# - exp (expiration timestamp)
```

### Common Debug Checks
- ✅ Token stored in localStorage
- ✅ Authorization header format: "Bearer TOKEN"
- ✅ Email in file matches user email (case-insensitive)
- ✅ File contains required email column
- ✅ Token hasn't expired (check exp claim)

---

## Quick Troubleshooting Checklist

- [ ] User is logged in (check localStorage for token)
- [ ] Token is not expired (check expiresIn)
- [ ] File contains user's email address
- [ ] Email in file is case-insensitive match
- [ ] Authorization header includes "Bearer " prefix
- [ ] POST request to correct endpoint
- [ ] Content-Type is multipart/form-data
- [ ] JWT_SECRET is set (min 32 chars)
- [ ] Database connection working
- [ ] Port 8080 is accessible

---

## Production Deployment

### Pre-Deployment Checklist
- [ ] Set strong JWT_SECRET environment variable
- [ ] Enable HTTPS/TLS
- [ ] Use httpOnly cookies for tokens (instead of localStorage)
- [ ] Set proper CORS headers
- [ ] Enable rate limiting on auth endpoints
- [ ] Configure proper logging
- [ ] Test token refresh under load
- [ ] Implement token revocation if needed

### Recommended Settings
```bash
# .env or environment configuration
JWT_SECRET=<32+ char random string from openssl rand -base64 32>
JWT_ACCESS_EXPIRATION=1800000        # 30 minutes
JWT_REFRESH_EXPIRATION=604800000     # 7 days (can be shorter)
SPRING_PROFILES_ACTIVE=production
```

