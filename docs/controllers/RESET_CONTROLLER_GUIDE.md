# Reset Controller - Testing & Development

## Overview

The `ResetController` provides REST endpoints to clear user-related data from the database. This is intended **for development and testing purposes only** and should be **disabled in production**.

⚠️ **WARNING**: These endpoints will permanently delete data. Use with caution.

## Endpoints

### Delete All User Data

**Endpoint**: `DELETE /api/reset/users`

**Description**: Clears all users, transactions, and holdings from the database.

**Response**:
```json
{
  "usersDeleted": 5,
  "transactionsDeleted": 10,
  "holdingsDeleted": 8,
  "status": "SUCCESS",
  "message": "All user-related data has been cleared"
}
```

**Status Code**: 200 OK

**Example using curl**:
```bash
curl -X DELETE http://localhost:8080/api/reset/users
```

### Health Check

**Endpoint**: `DELETE /api/reset/health`

**Description**: Returns a warning message indicating the reset controller is active (for testing only).

**Response**:
```
Reset controller is active. WARNING: This is for development/testing only. Ensure this endpoint is disabled in production.
```

**Status Code**: 200 OK

**Example using curl**:
```bash
curl -X DELETE http://localhost:8080/api/reset/health
```

## Usage Workflow for Testing

1. **Run initial tests** with seed data
2. **Delete user data** to reset state:
   ```bash
   curl -X DELETE http://localhost:8080/api/reset/users
   ```
3. **Run another round of tests** with fresh data
4. **Repeat** as needed

## Disabling in Production

To disable the reset controller in production environments:

### Option 1: Conditional Bean Registration
In your application configuration, register the `ResetController` and `ResetService` only in development/test profiles:

```java
@Configuration
@Profile({"dev", "test"})
public class ResetControllerConfig {
    // Register reset beans only in dev/test
}
```

### Option 2: Environment-based Endpoint Filtering
Use Spring Security or a filter to block `/api/reset/**` endpoints in production:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (isProduction()) {
            http.authorizeHttpRequests((requests) -> requests
                .requestMatchers("/api/reset/**").denyAll()
                .anyRequest().permitAll()
            );
        }
        return http.build();
    }
}
```

### Option 3: Remove Class from Production Build
Exclude `ResetController.java` and `ResetService.java` from production builds in your build configuration.

## Data Deletion Order

The reset operation deletes data in this order to respect foreign key constraints:

1. **User Transactions** (user_transaction table)
2. **User Holdings** (user_holding table)
3. **Portfolio Users** (portfolio_user table)

This ensures no foreign key violations occur during deletion.

## Logging

All reset operations are logged at WARN and INFO levels:

```
WARN  - Resetting all user-related data
INFO  - Deleted 10 user transactions
INFO  - Deleted 8 user holdings
INFO  - Deleted 5 users
```

## Important Notes

- ⚠️ This operation is **irreversible**. There is no backup or undo.
- 🔒 Ensure this endpoint is **never accessible in production**.
- 📊 The endpoint returns a summary of deleted records.
- 🔄 All deletions happen in a **single transaction** for consistency.

## Future Deprecation

This controller is marked with `@Deprecated(since = "0.1.0", forRemoval = true)`.

When the project is ready for production:
1. Remove this controller entirely, or
2. Replace it with a more sophisticated data reset mechanism, or
3. Move it to a separate admin/testing module

## See Also

- `ResetService.java` - Service that performs the data deletion
- `ResetController.java` - REST controller exposing reset endpoints
- Test classes: `ResetServiceTest.java`, `ResetControllerTest.java`

