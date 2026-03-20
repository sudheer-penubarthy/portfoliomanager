# Upload History Feature - Quick Reference

## Files Created/Modified

### Backend Files Created

#### 1. Entity
- `src/main/java/com/sudheer/portfoliotracker/infrastructure/persistence/entity/UploadHistory.java`
  - JPA entity for upload_history table
  - Maps userId, fileName, uploadDate, status, recordsProcessed, recordsFailed, errorMessage

#### 2. Enum
- `src/main/java/com/sudheer/portfoliotracker/enums/UploadStatus.java`
  - Enum: UPLOADED, PROCESSING, COMPLETED, FAILED
  - JSON-compatible with converters

#### 3. DTO
- `src/main/java/com/sudheer/portfoliotracker/api/dto/UploadHistoryDto.java`
  - Data Transfer Object for API responses
  - Same fields as entity except timestamps

#### 4. Repository
- `src/main/java/com/sudheer/portfoliotracker/repository/UploadHistoryRepository.java`
  - Method: `findByUserIdOrderByUploadDateDesc(Long userId)`
  - Spring Data JPA auto-generates SQL

#### 5. Service
- `src/main/java/com/sudheer/portfoliotracker/service/UploadHistoryService.java`
  - Method: `getUploadHistoryForUser(Long userId)`
  - Entity to DTO conversion
  - Read-only transactional

#### 6. Controller
- `src/main/java/com/sudheer/portfoliotracker/api/controller/UploadHistoryController.java`
  - Endpoint: `GET /api/uploads/history`
  - Extracts userId from JWT token
  - Returns List<UploadHistoryDto>

#### 7. Database Migration
- `src/main/resources/db/migration/V2__Create_Upload_History_Table.sql`
  - Creates upload_history table
  - Adds indexes and foreign key
  - Flyway auto-applies on startup

### Frontend Files Created

#### 1. Service
- `client/src/app/shared/services/upload.service.ts`
  - Interface: UploadHistory
  - Method: `getUploadHistory(): Observable<UploadHistory[]>`

#### 2. Routes
- `client/src/app/features/uploads/upload.routes.ts`
  - Route config with authGuard
  - Lazy loads component

#### 3. Component
- `client/src/app/features/uploads/pages/upload-history/upload-history.component.ts`
  - Loads history on init
  - Handles loading, error, empty states
  - Provides status color mapping
  - Date formatting

#### 4. Template
- `client/src/app/features/uploads/pages/upload-history/upload-history.component.html`
  - Material card wrapper
  - Material table with data
  - Status chips with icons
  - Error and empty states
  - Retry button

#### 5. Styles
- `client/src/app/features/uploads/pages/upload-history/upload-history.component.scss`
  - Material Design styling
  - Responsive layout
  - Status color coding
  - Loading/error/empty state styles

### Files Modified

#### Frontend
1. `client/src/app/app.routes.ts`
   - Added route: `/upload-history`

2. `client/src/app/shared/components/navbar/navbar.component.html`
   - Added Upload History button with cloud_upload icon

## API Endpoint

### GET /api/uploads/history
```bash
curl -H "Authorization: Bearer {token}" http://localhost:8080/api/uploads/history
```

**Response:**
```json
[
  {
    "id": 1,
    "fileName": "cams_jan_2024.csv",
    "uploadDate": "2024-01-15T10:30:45",
    "status": "COMPLETED",
    "recordsProcessed": 42,
    "recordsFailed": 0,
    "errorMessage": null
  }
]
```

## Feature Access

### URL
`http://localhost:4200/upload-history`

### Navigation
- Click "Upload History" button in navbar (cloud_upload icon)
- OR navigate to `/upload-history` directly (requires login)

## Status Codes

| Status | Color | Icon | Meaning |
|--------|-------|------|---------|
| UPLOADED | Blue | cloud_upload | File uploaded, waiting for processing |
| PROCESSING | Orange | schedule | File is being processed |
| COMPLETED | Green | check_circle | Processing completed successfully |
| FAILED | Red | cancel | Processing failed |

## Security

✅ **User Isolation:**
- Token userId extracted from JWT
- Repository filters by userId
- No data from other users accessible

✅ **Authentication:**
- Route protected by authGuard
- Token automatically injected by JwtInterceptor
- Invalid tokens rejected at controller

✅ **Data Flow:**
1. Frontend sends request with Authorization header
2. JwtInterceptor adds Bearer token
3. Controller extracts userId from token
4. Service queries only that user's data
5. Frontend displays only authenticated user's uploads

## Using in Other Components

### Get user's upload history in any component:
```typescript
import { UploadService } from '@shared/services/upload.service';

constructor(private uploadService: UploadService) {}

loadHistory() {
  this.uploadService.getUploadHistory().subscribe({
    next: (history) => console.log(history),
    error: (err) => console.error(err)
  });
}
```

### Update upload status (from upload handler):
```typescript
// After file upload completes
uploadHistoryRepository.save(
  UploadHistory.builder()
    .userId(userId)
    .fileName(fileName)
    .uploadDate(LocalDateTime.now())
    .status(UploadStatus.COMPLETED)
    .recordsProcessed(count)
    .recordsFailed(0)
    .build()
);
```

## Troubleshooting

### No data showing
1. Check if uploads exist in database: `SELECT * FROM upload_history WHERE user_id = ?`
2. Verify JWT token is valid in browser localStorage
3. Check browser console for API errors
4. Check backend logs for token validation errors

### Table not displaying
1. Verify Material modules are imported in component
2. Check browser console for Angular errors
3. Verify UploadService is provided in root

### Authorization errors
1. Verify JWT token is in Authorization header
2. Check token isn't expired: `token.expiresIn`
3. Verify token contains `userId` claim
4. Check JwtTokenService.validateToken() in backend

## Performance Notes

### Database
- Composite index on `(user_id, upload_date DESC)` optimizes queries
- Foreign key cascade delete removes all uploads when user deleted

### Frontend
- Component unsubscribes on destroy (no memory leaks)
- Lazy-loaded route (reduces initial bundle)
- OnPush change detection possible if needed

### Backend
- Read-only transactional (better performance)
- No N+1 queries (simple single query)
- Token validation cached in JwtTokenService

## Related Documentation

See `UPLOAD_HISTORY_IMPLEMENTATION.md` for:
- Detailed architecture
- Integration points
- Testing recommendations
- Future enhancements

