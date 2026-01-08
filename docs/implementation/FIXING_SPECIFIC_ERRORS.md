# Fixing Specific Errors from Your Logs

## Error 1: "Column 'updated_at' cannot be null"

### Problem
```
2025-12-08 03:16:03.584 [http-nio-8080-exec-1] ERROR o.h.e.jdbc.spi.SqlExceptionHelper
SQL Error: 1048, SQLState: 23000
Column 'updated_at' cannot be null
```

### Root Cause
Your entity has an `updated_at` column that doesn't allow null values, but it's being set to null during insert/update operations.

### Solutions

#### Solution A: Add Automatic Timestamp Update (Recommended)
In your entity class (e.g., `AmfiNav.java`, `AmfiScheme.java`):

```java
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "your_table")
public class YourEntity {
    // ... other fields ...
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

#### Solution B: Set in Service Layer
In your service before saving:

```java
@Service
public class YourService {
    @Autowired
    private YourRepository repository;
    
    public void saveOrUpdate(YourEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        repository.save(entity);
    }
}
```

#### Solution C: Update Database Schema
If the column should allow null:

```sql
ALTER TABLE your_table MODIFY updated_at DATETIME NULL;
```

## Error 2: "Argument [COMPLETED] did not match parameter type [Status]"

### Problem
```
{
  "error": "Argument [COMPLETED] of type [java.lang.String] did not match parameter type [com.example.portfoliotracker.enums.Status (n/a)]"
}
```

### Root Cause
You're sending a string value like `"COMPLETED"` but the API expects the Status enum. The framework can't automatically convert it.

### Solutions

#### Solution A: Add @JsonCreator to Status Enum (Recommended)
In `src/main/java/com/example/portfoliotracker/enums/Status.java`:

```java
import com.fasterxml.jackson.annotation.JsonCreator;

public enum Status {
    ACTIVE,
    COMPLETED,
    PENDING;
    
    @JsonCreator
    public static Status forValue(String value) {
        if (value == null) return null;
        try {
            return Status.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle invalid value
            throw new IllegalArgumentException(
                String.format("Invalid status: %s. Valid values are: ACTIVE, COMPLETED, PENDING", value)
            );
        }
    }
}
```

#### Solution B: Create Custom Converter
```java
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToStatusConverter implements Converter<String, Status> {
    @Override
    public Status convert(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Status.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("Invalid status: %s", value)
            );
        }
    }
}
```

#### Solution C: Validate in Controller
```java
@RestController
public class YourController {
    
    @PostMapping("/endpoint")
    public ResponseEntity<String> saveStatus(@RequestParam String status) {
        // Validate before use
        try {
            Status enumStatus = Status.valueOf(status.toUpperCase());
            // Use enumStatus...
            return ResponseEntity.ok("Success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body("Invalid status: " + status);
        }
    }
}
```

#### Client Usage (After Fix)
```bash
# Correct usage
curl -X POST http://localhost:8080/api/endpoint \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'

# Response with global handler
# HTTP 200 OK (if validation passes)
# HTTP 400 Bad Request (if validation fails with clear message)
```

## Error 3: "Failed to parse date: 08-OCT-2025"

### Problem
```
2025-12-08 03:41:49.460 [http-nio-8080-exec-8] ERROR c.e.p.service.CamsStreamParser
Ignored date parse error for format DD_MM_YYYY: 08-OCT-2025
Ignored date parse error for format DD_SLASH_MM_SLASH_YYYY: 08-OCT-2025
Failed to parse date: 08-OCT-2025
```

### Root Cause
Your `DateFormat` enum doesn't include the `DD-MMM-YYYY` format (with hyphens and 3-letter month abbreviation).

### Solution
In `src/main/java/com/example/portfoliotracker/enums/DateFormat.java`:

```java
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public enum DateFormat {
    DD_MM_YYYY("dd/MM/yyyy"),
    DD_SLASH_MM_SLASH_YYYY("dd/MM/yyyy"),
    DD_MMM_YYYY("dd-MMM-yyyy", Locale.ENGLISH),  // ADD THIS LINE
    YYYY_MM_DD("yyyy-MM-dd"),
    DD_MMMM_YYYY("dd-MMMM-yyyy", Locale.ENGLISH),
    ISO_DATE("yyyy-MM-dd");
    
    private final String pattern;
    private final Locale locale;
    private final DateTimeFormatter formatter;
    
    DateFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }
    
    DateFormat(String pattern, Locale locale) {
        this.pattern = pattern;
        this.locale = locale;
        this.formatter = DateTimeFormatter.ofPattern(pattern, locale);
    }
    
    public DateTimeFormatter getFormatter() {
        return formatter;
    }
}
```

### Testing the Fix
```java
// Test the parser
LocalDate date = LocalDate.parse("08-OCT-2025", 
    DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH));
System.out.println(date); // 2025-10-08
```

## How Global Exception Handler Helps

Once these errors occur, the global exception handler will catch them and return consistent responses:

### Error 1 Response (updated_at null)
```json
{
  "timestamp": "2025-12-08T03:16:03.584Z",
  "status": 409,
  "error": "Data Integrity Violation",
  "message": "Required field cannot be null",
  "path": "/api/amfi/endpoint"
}
```

### Error 2 Response (Enum conversion)
```json
{
  "timestamp": "2025-12-08T03:41:49.463Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Argument [COMPLETED] of type [java.lang.String] did not match parameter type [Status]",
  "path": "/api/your/endpoint"
}
```

### Error 3 Response (Date parsing)
```json
{
  "timestamp": "2025-12-08T03:41:49.465Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to parse date: 08-OCT-2025",
  "path": "/api/cams/upload"
}
```

## Testing Checklist

- [ ] Fix Status enum with @JsonCreator
- [ ] Fix DateFormat enum to include DD-MMM-YYYY
- [ ] Add @UpdateTimestamp to entity classes
- [ ] Test with valid inputs
- [ ] Test with invalid inputs and verify error responses
- [ ] Check logs for consistent error logging
- [ ] Verify error responses follow ApiError format

## Prevention Tips

1. **Input Validation**: Always validate user input before processing
2. **Type Safety**: Use enums for status/type fields instead of strings
3. **Date Handling**: Centralize date parsing with comprehensive format support
4. **Null Safety**: Use `@NotNull` annotation on required fields
5. **Logging**: The global handler logs all errors automatically
6. **Testing**: Write tests for edge cases and error scenarios

