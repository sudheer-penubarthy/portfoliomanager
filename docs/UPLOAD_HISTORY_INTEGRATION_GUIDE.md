# Upload History Integration Guide

## How to Integrate Upload History into Your Existing Upload System

This guide shows how to integrate the upload history feature with your file upload functionality.

## Backend Integration

### Step 1: Inject UploadHistoryRepository into Your Upload Controller/Service

```java
import com.sudheer.portfoliotracker.infrastructure.persistence.entity.UploadHistory;
import com.sudheer.portfoliotracker.enums.UploadStatus;
import com.sudheer.portfoliotracker.repository.UploadHistoryRepository;

@Service
public class YourUploadService {
    
    private final UploadHistoryRepository uploadHistoryRepository;
    
    public YourUploadService(UploadHistoryRepository uploadHistoryRepository) {
        this.uploadHistoryRepository = uploadHistoryRepository;
    }
}
```

### Step 2: Record Upload Start

When a file is uploaded, create an initial upload history record:

```java
public void handleFileUpload(MultipartFile file, Long userId) {
    String fileName = file.getOriginalFilename();
    
    // Create upload history record with PROCESSING status
    UploadHistory uploadHistory = UploadHistory.builder()
        .userId(userId)
        .fileName(fileName)
        .uploadDate(LocalDateTime.now())
        .status(UploadStatus.PROCESSING)
        .recordsProcessed(0)
        .recordsFailed(0)
        .build();
    
    UploadHistory savedRecord = uploadHistoryRepository.save(uploadHistory);
    
    // Continue with file processing...
    processFile(file, userId, savedRecord.getId());
}
```

### Step 3: Update Status During Processing

As you process the file, update the record with progress:

```java
private void processFile(MultipartFile file, Long userId, Long uploadHistoryId) {
    UploadHistory uploadHistory = uploadHistoryRepository.findById(uploadHistoryId)
        .orElseThrow(() -> new ResourceNotFoundException("Upload history not found"));
    
    try {
        int processedCount = 0;
        int failedCount = 0;
        
        // Your file processing logic
        List<Transaction> transactions = parseFile(file);
        
        for (Transaction transaction : transactions) {
            try {
                saveTransaction(transaction);
                processedCount++;
            } catch (Exception e) {
                failedCount++;
                log.warn("Failed to process transaction: {}", e.getMessage());
            }
        }
        
        // Update with final results
        uploadHistory.setStatus(UploadStatus.COMPLETED);
        uploadHistory.setRecordsProcessed(processedCount);
        uploadHistory.setRecordsFailed(failedCount);
        uploadHistory.setErrorMessage(null);
        uploadHistoryRepository.save(uploadHistory);
        
        log.info("Upload completed: {} processed, {} failed", processedCount, failedCount);
        
    } catch (Exception e) {
        // Mark as failed if upload encounters critical error
        uploadHistory.setStatus(UploadStatus.FAILED);
        uploadHistory.setErrorMessage(e.getMessage());
        uploadHistoryRepository.save(uploadHistory);
        
        log.error("Upload failed: {}", e.getMessage(), e);
        throw new UploadProcessingException("Failed to process upload: " + e.getMessage());
    }
}
```

### Step 4: Verify in API Endpoint

Ensure your existing upload controller extracts userId from JWT:

```java
@PostMapping("/upload")
public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                    @RequestHeader("Authorization") String authHeader) {
    // Extract userId from JWT token (using existing method)
    Long userId = extractUserIdFromToken(authHeader);
    
    // Your upload processing
    uploadService.handleFileUpload(file, userId);
    
    return ResponseEntity.ok("File uploaded successfully");
}

private Long extractUserIdFromToken(String authorizationHeader) {
    if (authorizationHeader == null || authorizationHeader.isEmpty()) {
        return null;
    }
    
    String token = authorizationHeader.substring("Bearer ".length());
    Claims claims = jwtTokenService.validateToken(token);
    
    if (claims == null) {
        return null;
    }
    
    return claims.get("userId", Long.class);
}
```

## Frontend Integration

### Step 1: Import UploadService in Your Upload Component

```typescript
import { UploadService } from '@shared/services/upload.service';

@Component({
  selector: 'app-file-upload',
  template: `...`
})
export class FileUploadComponent {
  constructor(private uploadService: UploadService) {}
}
```

### Step 2: Refresh Upload History After Upload

After successful file upload, trigger a refresh of the upload history:

```typescript
onFileSelected(event: any) {
  const file: File = event.target.files[0];
  
  if (file) {
    this.uploadFile(file);
  }
}

uploadFile(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  
  this.http.post('/api/upload', formData).subscribe({
    next: (response) => {
      console.log('File uploaded successfully');
      
      // Trigger history refresh if needed
      this.refreshUploadHistory();
      
      // Show success message
      this.showSuccessMessage('File uploaded successfully');
    },
    error: (error) => {
      console.error('Upload failed:', error);
      this.showErrorMessage('Upload failed: ' + error.error.message);
    }
  });
}

refreshUploadHistory() {
  // If you have a service to get history, refresh it here
  this.uploadService.getUploadHistory().subscribe({
    next: (history) => {
      // Update your component's history display
      this.uploadHistory = history;
    }
  });
}
```

### Step 3: Navigate to Upload History After Upload

Option 1: Automatic navigation after successful upload

```typescript
import { Router } from '@angular/router';

constructor(private router: Router) {}

uploadFile(file: File) {
  this.http.post('/api/upload', formData).subscribe({
    next: (response) => {
      // Navigate to upload history
      this.router.navigate(['/upload-history']);
      
      // Show toast notification
      this.showSuccessToast('File uploaded and processing has started');
    }
  });
}
```

Option 2: Optional navigation with user confirmation

```typescript
uploadFile(file: File) {
  this.http.post('/api/upload', formData).subscribe({
    next: (response) => {
      const goToHistory = confirm(
        'File uploaded successfully! View upload history?'
      );
      
      if (goToHistory) {
        this.router.navigate(['/upload-history']);
      }
    }
  });
}
```

## Example: Complete ZipHandler Integration

If you have a `ZipHandlerService` for processing ZIP files:

```java
@Service
public class ZipHandlerService {
    
    private final UploadHistoryRepository uploadHistoryRepository;
    
    public void processUploadedZip(MultipartFile zipFile, Long userId, Long uploadHistoryId) {
        UploadHistory uploadHistory = uploadHistoryRepository.findById(uploadHistoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Upload record not found"));
        
        try {
            // Unzip and process
            List<File> extractedFiles = unzipFile(zipFile);
            
            int totalProcessed = 0;
            int totalFailed = 0;
            
            for (File extractedFile : extractedFiles) {
                try {
                    int processed = processIndividualFile(extractedFile, userId);
                    totalProcessed += processed;
                } catch (Exception e) {
                    totalFailed++;
                    log.error("Failed to process file: {}", extractedFile.getName(), e);
                }
            }
            
            // Update upload history with final counts
            uploadHistory.setStatus(UploadStatus.COMPLETED);
            uploadHistory.setRecordsProcessed(totalProcessed);
            uploadHistory.setRecordsFailed(totalFailed);
            uploadHistoryRepository.save(uploadHistory);
            
        } catch (Exception e) {
            uploadHistory.setStatus(UploadStatus.FAILED);
            uploadHistory.setErrorMessage("ZIP processing failed: " + e.getMessage());
            uploadHistoryRepository.save(uploadHistory);
            throw new ProcessingException("ZIP processing failed", e);
        }
    }
}
```

## Testing the Integration

### 1. Upload a file through your API
```bash
curl -X POST http://localhost:8080/api/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/file.csv"
```

### 2. Verify record in database
```sql
SELECT * FROM upload_history 
WHERE user_id = ? 
ORDER BY upload_date DESC 
LIMIT 1;
```

### 3. View in Upload History UI
Navigate to `http://localhost:4200/upload-history`

### 4. Verify data displays correctly
- Check file name matches uploaded file
- Status should be COMPLETED (if processing succeeded) or FAILED
- Record counts should match processed records
- Error message should be visible if status is FAILED

## Error Handling Best Practices

### Backend Error Handling

```java
public void handleFileUpload(MultipartFile file, Long userId) {
    Long uploadHistoryId = null;
    
    try {
        // Create initial record
        UploadHistory uploadHistory = createUploadRecord(userId, file.getOriginalFilename());
        uploadHistoryId = uploadHistory.getId();
        
        // Process file
        processFile(file, userId, uploadHistoryId);
        
    } catch (ValidationException e) {
        updateUploadStatus(uploadHistoryId, UploadStatus.FAILED, 
            "Validation error: " + e.getMessage());
    } catch (IOException e) {
        updateUploadStatus(uploadHistoryId, UploadStatus.FAILED, 
            "File read error: " + e.getMessage());
    } catch (Exception e) {
        updateUploadStatus(uploadHistoryId, UploadStatus.FAILED, 
            "Unexpected error: " + e.getMessage());
    }
}

private void updateUploadStatus(Long uploadHistoryId, UploadStatus status, String errorMsg) {
    if (uploadHistoryId == null) return;
    
    UploadHistory history = uploadHistoryRepository.findById(uploadHistoryId)
        .orElse(null);
    if (history != null) {
        history.setStatus(status);
        history.setErrorMessage(errorMsg);
        uploadHistoryRepository.save(history);
    }
}
```

### Frontend Error Handling

```typescript
uploadFile(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  
  this.isUploading = true;
  
  this.http.post('/api/upload', formData).subscribe({
    next: (response) => {
      this.isUploading = false;
      this.showSuccessNotification('File uploaded successfully');
      this.router.navigate(['/upload-history']);
    },
    error: (error) => {
      this.isUploading = false;
      
      if (error.status === 401) {
        this.showErrorNotification('Session expired. Please login again.');
        this.router.navigate(['/login']);
      } else if (error.status === 400) {
        this.showErrorNotification('Invalid file: ' + error.error.message);
      } else if (error.status === 413) {
        this.showErrorNotification('File is too large. Maximum size is 100MB.');
      } else {
        this.showErrorNotification('Upload failed. Please try again.');
      }
    }
  });
}
```

## Performance Optimization Tips

### 1. Batch Process Records
For large files, process in batches:

```java
private static final int BATCH_SIZE = 100;

List<Transaction> transactions = parseFile(file);
for (int i = 0; i < transactions.size(); i += BATCH_SIZE) {
    int end = Math.min(i + BATCH_SIZE, transactions.size());
    List<Transaction> batch = transactions.subList(i, end);
    saveBatch(batch);
    
    // Optional: Update progress periodically
    if (i % 1000 == 0) {
        updateUploadProgress(uploadHistoryId, i, transactions.size());
    }
}
```

### 2. Use Index for Query Optimization
The table already has composite index:
```sql
INDEX idx_user_upload_date (user_id, upload_date DESC)
```

This ensures fast queries even with many uploads.

### 3. Async Processing (Optional)
For large files, consider async processing:

```java
@Async
public void processFileAsync(MultipartFile file, Long userId, Long uploadHistoryId) {
    // File processing happens in background thread
    // User gets immediate response
}

// In controller:
uploadService.processFileAsync(file, userId, uploadHistoryId);
return ResponseEntity.accepted().build();
```

## Next Steps

1. **Implement in Your Upload Flow:** Follow the integration steps above
2. **Test Thoroughly:** Test with various file sizes and formats
3. **Monitor Logs:** Watch for errors in processing
4. **Gather Feedback:** Get user feedback on the upload history UI
5. **Optimize:** Use performance tips for large-scale uploads

