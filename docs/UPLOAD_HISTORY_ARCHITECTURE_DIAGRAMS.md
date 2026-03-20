# Upload History Feature - Architecture Diagram

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ANGULAR FRONTEND (Client)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    Navigation (Navbar Component)                     │   │
│  │  Dashboard | Portfolio | Goals | [Upload History] | User Profile   │   │
│  └────────────────────────────────────────┬──────────────────────────┘   │
│                                           │                               │
│                                   User clicks button                       │
│                                           │                               │
│                                           ▼                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │           Upload History Route (/upload-history)                     │   │
│  │    Protected by authGuard (ensures user is authenticated)            │   │
│  └────────────────────────────────────────┬──────────────────────────┘   │
│                                           │                               │
│                                           ▼                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │         UploadHistoryComponent (TypeScript)                          │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ - loadUploadHistory()                                        │   │   │
│  │  │ - Handle Loading, Error, Empty states                       │   │   │
│  │  │ - Display data in Material table                            │   │   │
│  │  │ - Status color mapping                                      │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────────────────┬──────────────────────────┘   │
│                                           │                               │
│                          Calls getUploadHistory()                         │
│                                           │                               │
│                                           ▼                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │              UploadService (HTTP Client)                             │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ getUploadHistory(): Observable<UploadHistory[]>              │   │   │
│  │  │ GET /api/uploads/history                                     │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────────────────┬──────────────────────────┘   │
│                                           │                               │
│                              JwtInterceptor adds                           │
│                          Authorization header + Token                      │
│                                           │                               │
│                                    HTTP GET Request                        │
│                                           │                               │
└───────────────────────────────────────────┼───────────────────────────────┘
                                            │
                        ┌───────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT BACKEND (Server)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │           UploadHistoryController                                    │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ GET /api/uploads/history                                     │   │   │
│  │  │ @RequestHeader Authorization                                 │   │   │
│  │  │                                                               │   │   │
│  │  │ 1. Extract JWT token from header                             │   │   │
│  │  │ 2. Validate token using JwtTokenService                      │   │   │
│  │  │ 3. Extract userId from token claims                          │   │   │
│  │  │ 4. Call service layer                                        │   │   │
│  │  │ 5. Return List<UploadHistoryDto>                             │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────────────────┬──────────────────────────┘   │
│                                           │                               │
│                    Calls getUploadHistoryForUser(userId)                  │
│                                           │                               │
│                                           ▼                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │              UploadHistoryService                                    │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ getUploadHistoryForUser(Long userId)                         │   │   │
│  │  │ - Query uploads from repository                              │   │   │
│  │  │ - Convert entities to DTOs                                   │   │   │
│  │  │ - Handle errors and logging                                  │   │   │
│  │  │ - Return List<UploadHistoryDto>                              │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────────────────┬──────────────────────────┘   │
│                                           │                               │
│             Calls findByUserIdOrderByUploadDateDesc(userId)                │
│                                           │                               │
│                                           ▼                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │           UploadHistoryRepository (Spring Data JPA)                  │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ List<UploadHistory>                                          │   │   │
│  │  │ findByUserIdOrderByUploadDateDesc(Long userId)               │   │   │
│  │  │                                                               │   │   │
│  │  │ Auto-generates SQL:                                          │   │   │
│  │  │ SELECT * FROM upload_history                                 │   │   │
│  │  │ WHERE user_id = ?                                            │   │   │
│  │  │ ORDER BY upload_date DESC                                    │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────────────────┬────────────────��─────────┘   │
│                                           │                               │
│                            Query executes on DB                           │
│                                           │                               │
│                                           ▼                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │              UploadHistory Entity (JPA)                              │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ Maps to upload_history table                                 │   │   │
│  │  │ - id (Primary Key)                                           │   │   │
│  │  │ - userId (Foreign Key to portfolio_user)                     │   │   │
│  │  │ - fileName                                                   │   │   │
│  │  │ - uploadDate                                                 │   │   │
│  │  │ - status (Enum: UPLOADED, PROCESSING, COMPLETED, FAILED)    │   │   │
│  │  │ - recordsProcessed                                           │   │   │
│  │  │ - recordsFailed                                              │   │   │
│  │  │ - errorMessage                                               │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────────────────┬──────────────────────────┘   │
│                                           │                               │
└───────────────────────────────────────────┼───────────────────────────────┘
                                            │
                        ┌───────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      MYSQL DATABASE                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │              upload_history Table                                    │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │ Columns:                                                     │   │   │
│  │  │ - id (BIGINT, AUTO_INCREMENT, PRIMARY KEY)                  │   │   │
│  │  │ - user_id (BIGINT, NOT NULL, FOREIGN KEY)                   │   │   │
│  │  │ - file_name (VARCHAR(255), NOT NULL)                        │   │   │
│  │  │ - upload_date (TIMESTAMP, NOT NULL)                         │   │   │
│  │  │ - status (VARCHAR(20), NOT NULL)                            │   │   │
│  │  │ - records_processed (INT, DEFAULT 0)                        │   │   │
│  │  │ - records_failed (INT, DEFAULT 0)                           │   │   │
│  │  │ - error_message (VARCHAR(1000), NULLABLE)                   │   │   │
│  │  │ - created_at (TIMESTAMP)                                    │   │   │
│  │  │ - updated_at (TIMESTAMP)                                    │   │   │
│  │  │                                                               │   │   │
│  │  │ Indexes:                                                      │   │   │
│  │  │ - idx_user_id (user_id)                                      │   │   │
│  │  │ - idx_user_upload_date (user_id, upload_date DESC)           │   │   │
│  │  │                                                               │   │   │
│  │  │ Constraint:                                                   │   │   │
│  │  │ - FK to portfolio_user.id (CASCADE DELETE)                    │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Data Flow Sequence Diagram

```
User                Frontend            Backend              Database
│                     │                    │                    │
├─ Click "Upload      │                    │                    │
│  History" btn       │                    │                    │
│                     │                    │                    │
│                     │ Navigate to        │                    │
│                     │ /upload-history    │                    │
│                     │◄──────────────────→| (authGuard check) │
│                     │                    │                    │
│                     │ GET /api/           │                    │
│                     │ uploads/history     │                    │
│                     │ + JWT Token         │                    │
│                     │───────────────────→│                    │
│                     │                    │ Extract userId     │
│                     │                    │ from JWT           │
│                     │                    │                    │
│                     │                    │ Query uploads      │
│                     │                    │ for this user      │
│                     │                    │───────────────────→│
│                     │                    │                    │
│                     │                    │ Find records       │
│                     │                    │ where user_id = ?  │
│                     │                    │ ORDER BY date DESC │
│                     │                    │                    │
│                     │                    │ Return results     │
│                     │                    │←───────────────────│
│                     │                    │                    │
│                     │                    │ Convert to DTOs    │
│                     │                    │                    │
│                     │ 200 OK              │                    │
│                     │ [UploadHistoryDto] │                    │
│                     │←───────────────────│                    │
│                     │                    │                    │
│                     │ Display in table   │                    │
│◄────────────────────│                    │                    │
│                     │                    │                    │
```

## Component Dependency Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    App Routes                                │
│  - Defines /upload-history route                             │
│  - Applies authGuard protection                              │
│  - Lazy loads UploadHistoryComponent                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              UploadHistoryComponent                          │
│  - Uses UploadService                                        │
│  - Imports Material modules                                  │
│  - Handles UI state (loading, error, empty, data)            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  UploadService                               │
│  - Calls HttpClient                                          │
│  - Decorated with JwtInterceptor                             │
│  - Returns Observable<UploadHistory[]>                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
              HTTP (with JWT token added)
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│            UploadHistoryController                           │
│  - Extracts JWT token                                        │
│  - Validates token                                           │
│  - Extracts userId from claims                               │
│  - Calls UploadHistoryService                                │
│  - Returns List<UploadHistoryDto>                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│             UploadHistoryService                             │
│  - Queries repository with userId                            │
│  - Converts entities to DTOs                                 │
│  - Returns results                                           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│           UploadHistoryRepository                            │
│  - JPA interface                                             │
│  - findByUserIdOrderByUploadDateDesc(userId)                │
│  - Executes SQL query                                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              UploadHistory Entity                            │
│  - Maps to upload_history table                              │
│  - JPA managed                                               │
│  - Contains all upload data                                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│            MySQL Database (upload_history)                   │
│  - Persists upload records                                   │
│  - Indexed for performance                                   │
│  - Foreign key to portfolio_user                             │
└─────────────────────────────────────────────────────────────┘
```

## Security Flow Diagram

```
Frontend (Angular)              Backend (Spring Boot)        JWT Token
│                               │                             │
│ 1. User logs in               │                             │
│ ────────────────────────────→ │                             │
│                               │ Generate JWT token          │
│                               │ (contains userId, email)    │
│                               │                             │
│ Receive token                 │                             │
│ Store in localStorage          │                             │
│                               │                             │
│ 2. Click "Upload History"     │                             │
│                               │                             │
│ 3. JwtInterceptor adds token  │                             │
│    to request header          │                             │
│                               │                             │
│ GET /api/uploads/history      │                             │
│ Authorization: Bearer {token} │                             │
│ ────────────────────────────→ │                             │
│                               │ 4. Extract token from      │
│                               │    Authorization header     │
│                               │    Bearer eyJhbGc...       │
│                               │                             │
│                               │ 5. Validate token          │
│                               │    using JwtTokenService   │
│                               │                    ────────→
│                               │                    │ Check  │
│                               │                    │ signature
│                               │                    │ Check  │
│                               │                    │ expiry │
│                               │                    ←────────
│                               │    ✓ Valid token           │
│                               │                             │
│                               │ 6. Extract userId from     │
│                               │    token claims            │
│                               │    userId = 42             │
│                               │                             │
│                               │ 7. Query database with     │
│                               │    userId = 42 ONLY        │
│                               │    ──────────────────────→
│                               │                   SELECT * FROM
│                               │                   upload_history
│                               │                   WHERE user_id = 42
│                               │                   ←───────────────
│                               │    Results: [Upload1, 2, 3]│
│                               │                             │
│ 200 OK                        │                             │
│ [UploadHistoryDto[], ...]     │                             │
│ ←────────────────────────────  │                             │
│                               │                             │
│ 8. Display in table           │                             │
│ Only user 42's uploads shown  │                             │
│                               │                             │
```

## Status State Machine

```
       Upload Started
            │
            ▼
    ┌───────────────┐
    │    UPLOADED   │  File uploaded to server
    │   (Blue chip) │  Waiting to be processed
    └───────┬───────┘
            │
            ▼
    ┌───────────────┐
    │  PROCESSING   │  File is being parsed
    │ (Orange chip) │  Records are being saved
    └───────┬───────┘
            │
        ┌───┴────────────────┐
        │                    │
        ▼                    ▼
    ┌──────────┐        ┌────────┐
    │COMPLETED │        │ FAILED │
    │ (Green)  │        │ (Red)  │
    └──────────┘        └────────┘
    All records              Some records
    processed                failed
    successfully             (error_message set)
```

## Technology Stack Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                  FRONTEND (Client)                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Angular 17+                                                 │
│  ├── Standalone Components                                  │
│  ├── Routing Module                                         │
│  ├── Guards (authGuard)                                     │
│  └── HTTP Client                                            │
│                                                              │
│  Angular Material                                            │
│  ├── mat-card                                                │
│  ├── mat-table                                               │
│  ├── mat-progress-spinner                                    │
│  ├── mat-chip                                                │
│  └── mat-icon                                                │
│                                                              │
│  RxJS                                                        │
│  ├── Observable                                              │
│  └── Operators (takeUntil, etc.)                             │
│                                                              │
│  TypeScript                                                  │
│  └── Strong typing                                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  BACKEND (Server)                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Spring Boot 3.x                                             │
│  ├── REST API Controller                                    │
│  ├── Service Layer                                          │
│  └── Dependency Injection                                   │
│                                                              │
│  JPA/Hibernate                                               │
│  ├── Entity Mapping                                         │
│  ├── Query Methods                                          │
│  └── Repository Pattern                                     │
│                                                              │
│  Spring Data JPA                                             │
│  └── Repository Interface                                   │
│                                                              │
│  JWT (JSON Web Tokens)                                       │
│  ├── Token Validation                                       │
│  └── Claims Extraction                                      │
│                                                              │
│  Lombok                                                      │
│  └── Boilerplate Reduction                                  │
│                                                              │
│  Flyway                                                      │
│  └── Database Migrations                                    │
│                                                              │
│  Swagger/OpenAPI                                             │
│  └── API Documentation                                      │
│                                                              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  DATABASE                                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  MySQL 5.7+                                                  │
│  ├── upload_history Table                                   │
│  ├── Indexes (user_id, user_id+upload_date)                │
│  ├── Foreign Key Constraints                                │
│  └── Cascade Delete                                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

All diagrams show the complete integration of the Upload History feature across the frontend, backend, and database layers.

