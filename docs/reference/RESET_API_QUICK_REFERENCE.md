# Reset API Quick Reference

## Endpoint Summary

| Method | Path | Purpose | Response |
|--------|------|---------|----------|
| `DELETE` | `/api/reset/users` | Clear all user, transaction, and holding data | JSON with deletion counts |
| `DELETE` | `/api/reset/health` | Check if reset controller is active | Warning message |

## Quick Examples

### Clear All User Data
```bash
curl -X DELETE http://localhost:8080/api/reset/users
```

**Response (200 OK)**:
```json
{
  "usersDeleted": 5,
  "transactionsDeleted": 15,
  "holdingsDeleted": 8,
  "status": "SUCCESS",
  "message": "All user-related data has been cleared"
}
```

### Check Reset Controller Status
```bash
curl -X DELETE http://localhost:8080/api/reset/health
```

**Response (200 OK)**:
```
Reset controller is active. WARNING: This is for development/testing only. Ensure this endpoint is disabled in production.
```

## Using in Test Workflow

```bash
# 1. Start fresh
curl -X DELETE http://localhost:8080/api/reset/users

# 2. Run your tests
# ... your test commands ...

# 3. Check logs for reset confirmation:
# WARN: Resetting all user-related data
# INFO: Deleted X user transactions
# INFO: Deleted Y user holdings  
# INFO: Deleted Z users
```

## Important Notes

- ✅ All deletions are **permanent** - no undo
- ✅ Data is deleted in order of FK constraints
- ✅ Operation is **atomic** (all or nothing)
- ⚠️ **For testing/development only** - disable in production
- 📊 Always returns count of deleted records

## Response Structure

```json
{
  "usersDeleted": <number>,           // Count of deleted users
  "transactionsDeleted": <number>,    // Count of deleted transactions
  "holdingsDeleted": <number>,        // Count of deleted holdings
  "status": "SUCCESS",                // Operation status
  "message": "..."                    // Descriptive message
}
```

## Disabling in Production

Before deploying to production, ensure this endpoint is **disabled** by:

1. Removing the `ResetController.java` and `ResetService.java` files, OR
2. Configuring Spring profiles to exclude these beans in production, OR
3. Using Spring Security to block `/api/reset/**` endpoints

## Performance

- Deletion order respects foreign key constraints
- Single transaction for consistency
- Typical execution time: < 1 second for small datasets

## Logging

When the reset endpoint is called, you'll see:
```
WARN - Resetting all user-related data
INFO - Deleted 10 user transactions
INFO - Deleted 8 user holdings
INFO - Deleted 5 users
```

## Tables Affected

- `portfolio_user` → deleted last (parent table)
- `user_holding` → deleted second (child of user)
- `user_transaction` → deleted first (child of user)

---

See `RESET_CONTROLLER_GUIDE.md` for detailed documentation.

