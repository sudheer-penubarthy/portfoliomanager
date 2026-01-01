# Global Exception Handler Implementation Guide

## Overview
A comprehensive global exception handler has been implemented using Spring's `@ControllerAdvice` to provide consistent error responses across the entire application.

## What Was Changed

### 1. **GlobalExceptionHandler.java** - Enhanced Exception Handling
Located at: `src/main/java/com/example/portfoliotracker/exception/GlobalExceptionHandler.java`

#### Features Added:
- **@ControllerAdvice**: Centralized exception handling for all controllers
- **@Slf4j**: Logging support for all exceptions
- **9 Exception Handlers** for different error scenarios

#### Exception Handlers Implemented:

| Exception Type | HTTP Status | Use Case |
|---|---|---|
| `ResourceNotFoundException` | 404 NOT_FOUND | When a requested resource is not found |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | Request body validation failures |
| `MethodArgumentTypeMismatchException` | 400 BAD_REQUEST | Type conversion failures (e.g., enum parsing) |
| `HttpMessageNotReadableException` | 400 BAD_REQUEST | Malformed JSON in request body |
| `IllegalArgumentException` | 400 BAD_REQUEST | Invalid arguments passed to methods |
| `ConstraintViolationException` | 409 CONFLICT | Database constraint violations |
| `DataIntegrityViolationException` | 409 CONFLICT | Data integrity violations (wrapper) |
| `Exception` (Generic) | 500 INTERNAL_SERVER_ERROR | All other unexpected errors |

### 2. **AmfiController.java** - Cleanup
- Removed try-catch block from `triggerSync()` method
- Exception handling now delegated to global handler
- Cleaner, more maintainable code

## Error Response Format

All error responses follow a consistent JSON structure defined by `ApiError`:

```json
{
  "timestamp": "2025-12-27T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Argument [COMPLETED] of type [java.lang.String] did not match parameter type [Status]",
  "path": "/api/amfi/funds",
  "details": ["field1: error message 1", "field2: error message 2"]
}
```

### ApiError Fields:
- `timestamp`: ISO 8601 timestamp of when the error occurred
- `status`: HTTP status code
- `error`: Error category/type
- `message`: Detailed error message
- `path`: Request path that caused the error
- `details`: List of specific validation errors (optional)

## How It Works

### Request Flow:
1. HTTP Request arrives at a controller
2. If an exception occurs during request processing
3. The `@ControllerAdvice` handler intercepts the exception
4. Appropriate `@ExceptionHandler` method is invoked
5. Exception is logged
6. Consistent `ApiError` response is returned

### Example: Type Mismatch Error

**Before (Old Code):**
```
[http-nio-8080-exec-8] ERROR c.e.p.service.CamsStreamParser
"error": "Argument [COMPLETED] of type [java.lang.String] did not match parameter type [com.example.portfoliotracker.enums.Status (n/a)]"
```

**After (With Global Handler):**
```json
{
  "timestamp": "2025-12-27T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Argument [COMPLETED] of type [java.lang.String] did not match parameter type [Status]",
  "path": "/api/amfi/endpoint"
}
```

## Fixing the Original Errors

### 1. **"Column 'updated_at' cannot be null"**
- **Root Cause**: Entity missing default timestamp or null assignment
- **Solution**: The global handler now catches `DataIntegrityViolationException` and returns HTTP 409 CONFLICT with a user-friendly message
- **Fix Entity**: Add `@UpdateTimestamp` or set default in service layer

### 2. **"Argument [COMPLETED]... did not match parameter type [Status]"**
- **Root Cause**: String enum value not converted to enum
- **Solution**: The `handleMethodArgumentTypeMismatch` handler catches this and returns HTTP 400
- **Fix Options**:
  - Add `@JsonCreator` to your Status enum
  - Use custom converter
  - Validate input before sending

### 3. **"Failed to parse date: 08-OCT-2025"**
- **Root Cause**: Date format parser missing hyphenated month abbreviation format
- **Solution**: Fix the DateFormat enum or parser to handle `DD-MMM-YYYY` format
- **File**: `src/main/java/com/example/portfoliotracker/enums/DateFormat.java`

## Usage Examples

### Accessing the Global Handler
The handler is automatically activated by Spring when you add `@ControllerAdvice` to the class. No additional configuration needed.

### Testing Exception Handling

#### Test 1: Invalid JSON
```bash
curl -X POST http://localhost:8080/api/amfi/sync \
  -H "Content-Type: application/json" \
  -d "{invalid json"
```
**Response**: HTTP 400 with "Invalid or malformed JSON" message

#### Test 2: Type Mismatch
```bash
curl -X GET "http://localhost:8080/api/amfi/nav/ABC?date=invalid-date"
```
**Response**: HTTP 400 with type mismatch message

#### Test 3: Resource Not Found
```bash
curl -X GET "http://localhost:8080/api/amfi/nav/NONEXISTENT?date=2025-12-27"
```
**Response**: HTTP 404 with "Not Found" message

## Best Practices

1. **Let Exceptions Bubble Up**: Don't catch exceptions in controllers; let the global handler manage them
2. **Use Appropriate Exception Types**: Throw `ResourceNotFoundException` for missing resources, etc.
3. **Log Appropriately**: The handler logs errors automatically; avoid duplicate logging in services
4. **Validation**: Use `@Valid` on request objects to leverage `MethodArgumentNotValidException` handler
5. **Custom Exceptions**: Create custom exception classes extending `RuntimeException` and add handlers for them

## Future Enhancements

Consider adding handlers for:
- `EntityNotFoundException` (JPA)
- `OptimisticLockingFailureException` (Concurrent updates)
- `BindException` (Form binding errors)
- `AccessDeniedException` (Security)
- Custom business exceptions

## Configuration

The global exception handler is configured in:
- Package: `com.example.portfoliotracker.exception`
- Class: `GlobalExceptionHandler`
- Annotation: `@ControllerAdvice`

No additional Spring configuration is needed. Spring automatically detects the `@ControllerAdvice` class.

## Related Files

1. **GlobalExceptionHandler.java** - Main handler class
2. **ApiError.java** - Response DTO
3. **ResourceNotFoundException.java** - Custom exception
4. **AmfiController.java** - Example usage

## Testing Checklist

- [ ] Test invalid JSON in request body
- [ ] Test enum conversion failures  
- [ ] Test missing required parameters
- [ ] Test database constraint violations (null fields)
- [ ] Test missing resources (404)
- [ ] Test generic exceptions (500)
- [ ] Verify all errors are logged
- [ ] Verify response format is consistent

