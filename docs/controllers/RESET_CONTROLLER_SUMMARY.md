# Reset Controller Implementation Summary

## Overview
A new `ResetController` has been added to the project for clearing user-related test data. This allows for multiple rounds of testing without needing to reinitialize the entire database.

⚠️ **IMPORTANT**: This controller is marked as `@Deprecated` and is for **development/testing only**. It must be disabled or removed before deploying to production.

## Files Added

### Service Layer
- **`src/main/java/com/example/portfoliotracker/service/ResetService.java`**
  - Service that handles the actual data deletion
  - Deletes in order: transactions → holdings → users (respects FK constraints)
  - Returns a `ResetSummary` DTO with deletion counts
  - All operations wrapped in a `@Transactional` method for consistency

- **Inner Class**: `ResetSummary`
  - DTO containing:
    - `usersDeleted` (long)
    - `transactionsDeleted` (long)
    - `holdingsDeleted` (long)
    - `status` (String) - always "SUCCESS"
    - `message` (String) - descriptive message

### Controller Layer
- **`src/main/java/com/example/portfoliotracker/controller/ResetController.java`**
  - REST endpoints for triggering data reset
  - Routes:
    - `DELETE /api/reset/users` - Clear all user-related data
    - `DELETE /api/reset/health` - Health check (returns warning message)
  - Marked with `@Deprecated` and `@RequestMapping("/api/reset")`

### Test Files
- **`src/test/java/com/example/portfoliotracker/service/ResetServiceTest.java`**
  - Unit tests for `ResetService`
  - Tests verify:
    - Correct deletion counts returned
    - Correct deletion order (transactions first, then holdings, then users)
    - Handling of empty data

- **`src/test/java/com/example/portfoliotracker/controller/ResetControllerTest.java`**
  - MockMvc tests for `ResetController`
  - Tests verify:
    - `/api/reset/users` endpoint returns correct JSON response
    - `/api/reset/health` endpoint returns warning message

### Documentation
- **`RESET_CONTROLLER_GUIDE.md`**
  - Comprehensive guide on using the reset controller
  - Includes:
    - Endpoint descriptions
    - Example curl commands
    - How to disable in production
    - Data deletion order
    - Important security notes

## Repositories Used
The service depends on these pre-existing repositories:
- `PortfolioUserRepository` - manages users
- `UserTransactionRepository` - manages transactions
- `UserHoldingRepository` - manages holdings

## How to Use

### From the API
```bash
# Clear all user data
curl -X DELETE http://localhost:8080/api/reset/users

# Check health/warning
curl -X DELETE http://localhost:8080/api/reset/health
```

### From the Application
1. User data is cleared instantly
2. Application responds with a summary of deleted records
3. Ready for new test data to be created

## Deletion Order
The service respects database foreign key constraints:
1. Delete all `user_transaction` records
2. Delete all `user_holding` records
3. Delete all `portfolio_user` records

This prevents foreign key constraint violations.

## Logging
All reset operations are logged:
- `WARN` level when reset is requested
- `INFO` level for each delete operation with count

## Security Considerations

### ⚠️ Production Safety
- Controller is marked `@Deprecated(forRemoval = true)`
- Should be **completely removed** before production deployment
- If needed to keep for ops, disable via:
  - Spring Security rules
  - Profile-based configuration
  - Environment-based feature flags

### Audit Trail
- All reset operations are logged at WARN/INFO level
- Can be tracked in application logs for compliance

## Testing

### Unit Tests Included
✅ `ResetServiceTest.java` - 2 test cases
- Test data deletion with counts
- Test handling of empty database

✅ `ResetControllerTest.java` - 2 test cases
- Test `/api/reset/users` endpoint response
- Test `/api/reset/health` endpoint response

### Running Tests
```bash
./gradlew.bat test
```

## Next Steps for Production Readiness

When preparing for production:

1. **Option A**: Remove completely
   - Delete `ResetController.java`
   - Delete `ResetService.java`
   - Remove test files

2. **Option B**: Conditional Registration
   - Keep but only register in `dev` and `test` profiles
   - Use Spring `@Profile` annotation

3. **Option C**: Access Control
   - Keep but restrict via Spring Security
   - Allow only specific user roles/IPs

## Files Modified
- None (all new files added)

## Files Created
- 2 main source files (service + controller)
- 2 test files
- 1 documentation file

## Status
✅ Implementation complete
✅ Tests passing
✅ Ready for development/testing use
⚠️ Requires removal/disabling before production

---

For detailed usage information, see `RESET_CONTROLLER_GUIDE.md`

