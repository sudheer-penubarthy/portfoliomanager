# AGENTS.md - AI Coding Agent Guide for Portfolio Manager

## Project Overview

Portfolio Manager is a full-stack investment tracking application built with:
- **Backend**: Java 17 + Spring Boot 3.2 + MySQL 8.0 + Flyway migrations
- **Frontend**: Angular 21 + Angular Material + RxJS
- **Key Features**: AMFI mutual fund data ingestion, portfolio management, upload history tracking, JWT authentication

---

## Architecture Patterns & Big Picture

### Layered Architecture

The backend follows **clean architecture** with distinct layers:

```
API Layer → Service Layer → Repository Layer → Database (with Flyway migrations)
  (Controllers)  (Business Logic)   (Data Access)
```

**Critical Detail**: Flyway manages ALL schema migrations. `application.yml` sets `hibernate.ddl-auto: none` to prevent Hibernate from trying to create tables. **Never** let Hibernate auto-create schema—always create Flyway SQL migrations (e.g., `src/main/resources/db/migration/V4__Add_New_Table.sql`).

### Package Structure

```
src/main/java/com/sudheer/portfoliotracker/
├── api/
│   ├── controller/          # REST endpoints (@RestController)
│   └── dto/                 # Data Transfer Objects (API contracts)
├── application/
│   ├── mapper/              # MapStruct entities ↔ DTOs (auto-generated)
│   └── service/impl/        # Specific service implementations
├── domain/
│   ├── model/               # JPA entities (@Entity) - where Hibernate persists
│   ├── policy/              # Business rules/policies
│   ├── port/                # Repository interfaces (Spring Data JPA)
│   └── service/             # Domain services (core business logic)
├── config/                  # Spring configuration (@Configuration)
├── exception/               # Custom exceptions
├── infrastructure/          # External integrations (API clients, file parsing)
├── jobs/                    # Scheduled tasks (@Scheduled)
├── util/                    # Helper utilities
└── PortfolioTrackerApplication.java  # Main Spring Boot entry point
```

### Data Flow Example: Uploading Transactions

1. **Frontend** (Angular): User uploads CSV → `UploadService` calls `POST /api/users/upload-transactions`
2. **Controller** (AuthController pattern): Extract JWT token from `Authorization` header → validate → extract `userId`
3. **Service** (e.g., `TransactionIngestService`): Parse CSV file → validate email matches user → persist records
4. **Repository**: Spring Data JPA auto-generates SQL with parameterized queries (SQL injection safe)
5. **Database**: Flyway ensures schema exists; records inserted into correct table

**Key Pattern**: User ID always comes from JWT token claims, NEVER from request body. This ensures data isolation.

---

## Security Model: User Isolation via JWT

All user-facing endpoints **must** require Bearer JWT token authentication:

```java
@RestController
@RequestMapping("/api/uploads")
public class UploadHistoryController {
    @GetMapping("/history")
    public ResponseEntity<List<UploadHistoryDto>> getUploadHistory(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        // 1. Extract token (remove "Bearer " prefix)
        String token = authHeader.replace("Bearer ", "");
        
        // 2. Validate and extract userId from JWT claims
        Long userId = jwtTokenService.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        
        // 3. Pass userId to service (never from request)
        return ResponseEntity.ok(uploadHistoryService.getUploadHistoryForUser(userId));
    }
}
```

**Repository must filter by userId**:
```java
public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
    List<UploadHistory> findByUserIdOrderByUploadDateDesc(Long userId);
    // Always filters by userId - no cross-user data access possible
}
```

**Frontend**: `JwtInterceptor` automatically adds token to all requests; `authGuard` protects routes.

---

## Critical Build & Deployment Commands

### Backend (Windows cmd.exe)

```bash
# Clean & build (runs all tests)
.\gradlew.bat clean build

# Start app (develops runs on http://localhost:8080)
.\gradlew.bat bootRun

# Run specific tests
.\gradlew.bat test --tests "*AmfiSyncControllerTest"

# Build for production (no tests)
.\gradlew.bat clean assemble

# Run migrations only (useful for debugging)
# (Flyway auto-runs on bootRun, but set this in IDE run config if needed)
```

**Important**: Windows uses `gradlew.bat` (not `gradlew`). If using PowerShell, prefix with `.\` even though cmd.exe doesn't require it.

### Frontend

```bash
cd client
npm install  # Install dependencies (Angular 21, Material)
npm start    # Dev server + auto-reload (http://localhost:4200)
npm build:prod  # Production build
npm test     # Karma test runner
```

### Database Setup

Create MySQL database first:
```sql
CREATE DATABASE portfoliomanager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Then set environment variables before running backend:
```bash
set DB_URL=jdbc:mysql://localhost:3306/portfoliomanager?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
set DB_USER=devuser
set DB_PASS=devpass
set JWT_SECRET=your-super-secret-key-minimum-32-characters-long-for-hs256-algorithm
.\gradlew.bat bootRun
```

Flyway will auto-create schema from `src/main/resources/db/migration/V*.sql` files.

---

## Configuration & Environment

### application.yml - Key Sections

**Database** (uses env vars):
```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/...}
    username: ${DB_USER:devuser}
    password: ${DB_PASS:devpass}
  jpa:
    hibernate:
      ddl-auto: none  # ← CRITICAL: Flyway manages schema, not Hibernate
      naming:
        physical-strategy: org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
```

**Flyway** (auto-runs on startup):
```yaml
flyway:
  enabled: true
  locations: classpath:db/migration  # SQL files: V1__.sql, V2__.sql, etc.
  baseline-on-migrate: true
  baseline-version: 1
  out-of-order: true  # Allows fixing migrations
  repair-on-migrate: true
```

**JWT** (authentication):
```yaml
jwt:
  secret: ${JWT_SECRET:...}
  access-token-expiration: 1800000    # 30 minutes
  refresh-token-expiration: 604800000  # 7 days
```

**AMFI Scheduled Jobs**:
```yaml
amfi:
  daily:
    cron: "0 30 20 * * ?"  # Daily at 8:30 PM UTC
  nav:
    url: https://portal.amfiindia.com/spages/NAVAll.txt
    connection-timeout-ms: 20000
```

---

## Common Workflows & Conventions

### Adding a New API Endpoint

1. **Create/Update DTO** (`src/main/java/.../dto/MyRequestDto.java`):
   ```java
   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   public class MyRequestDto {
       private String field;
   }
   ```
   
2. **Create/Update Entity** (`src/main/java/.../domain/model/MyEntity.java`):
   ```java
   @Entity
   @Table(name = "my_entities")
   @Data
   @AllArgsConstructor
   @NoArgsConstructor
   public class MyEntity {
       @Id
       @GeneratedValue(strategy = GenerationType.IDENTITY)
       private Long id;
       private String field;
       private Long userId;  // Always include for user isolation
   }
   ```

3. **Create Repository** (`src/main/java/.../domain/port/MyRepository.java`):
   ```java
   public interface MyRepository extends JpaRepository<MyEntity, Long> {
       List<MyEntity> findByUserId(Long userId);
   }
   ```

4. **Create Service** (`src/main/java/.../domain/service/MyService.java`):
   ```java
   @Service
   @RequiredArgsConstructor  // Lombok: constructor injection
   public class MyService {
       private final MyRepository myRepository;
       
       public MyEntity save(MyEntity entity) {
           return myRepository.save(entity);
       }
   }
   ```

5. **Create/Update Controller** (`src/main/java/.../api/controller/MyController.java`):
   ```java
   @RestController
   @RequestMapping("/api/my-resource")
   @RequiredArgsConstructor
   public class MyController {
       private final MyService myService;
       private final JwtTokenService jwtTokenService;
       
       @PostMapping
       public ResponseEntity<MyResponseDto> create(
               @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
               @RequestBody MyRequestDto request) {
           Long userId = jwtTokenService.extractUserId(authHeader.replace("Bearer ", ""));
           // ... use userId to isolate data
       }
   }
   ```

6. **Create Flyway Migration** (`src/main/resources/db/migration/V<X>__Description.sql`):
   ```sql
   CREATE TABLE IF NOT EXISTS my_entities (
       id BIGINT AUTO_INCREMENT PRIMARY KEY,
       user_id BIGINT NOT NULL,
       field VARCHAR(255),
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
       INDEX idx_user_id (user_id)
   );
   ```

### Testing Patterns

- **Unit Tests**: Mock repositories & services; test business logic in isolation
- **Integration Tests**: Use `@SpringBootTest` with test database (H2 or test MySQL)
- **Test Name Convention**: `*Test.java` → auto-discovered by Gradle `test` task
- **Code Coverage**: JaCoCo configured; run `./gradlew jacocoTestReport` to see coverage

---

## Frontend Patterns (Angular 21)

### Module Organization

```
client/src/app/
├── app.routes.ts            # Route definitions (standalone routing)
├── app.component.ts         # Root component
├── features/                # Feature modules (lazy-loaded)
│   ├── auth/               # Login, Register, etc.
│   ├── portfolio/          # Portfolio management
│   └── upload-history/     # Upload tracking
├── shared/
│   ├── services/           # HTTP services (Singleton via providedIn: 'root')
│   ├── guards/             # Route guards (authGuard)
│   ├── interceptors/       # HTTP interceptors (JwtInterceptor)
│   └── models/             # TypeScript interfaces
```

### HTTP Service Pattern

```typescript
@Injectable({ providedIn: 'root' })
export class MyService {
  constructor(private http: HttpClient) {}
  
  getItems(): Observable<MyDto[]> {
    return this.http.get<MyDto[]>('/api/my-resource');
    // JwtInterceptor auto-adds Bearer token
  }
}
```

### Component Pattern

```typescript
@Component({
  selector: 'app-my-feature',
  templateUrl: './my-feature.component.html',
  styleUrls: ['./my-feature.component.scss']
})
export class MyFeatureComponent implements OnInit {
  items$ = new Observable<MyDto[]>();
  loading = signal(false);
  
  constructor(private myService: MyService) {}
  
  ngOnInit() {
    this.loading.set(true);
    this.items$ = this.myService.getItems().pipe(
      finalize(() => this.loading.set(false))
    );
  }
}
```

---

## Integration Points & External Services

### AMFI Mutual Fund Data Ingestion

- **Service**: `AmfiIngestService` + `AmfiParser`
- **Source**: https://portal.amfiindia.com/spages/NAVAll.txt (text file)
- **Trigger**: Scheduled job (cron) OR manual `POST /api/amfi/sync`
- **Pattern**: Download → Parse CSV-like format → Persist to `amfi_schemes` + `amfi_navs` tables
- **Key Method**: `fetchAndIngest()` runs transactionally; idempotent (updates existing)

### File Upload Parsing

- **Services**: `ZipHandlerService`, `CamsStreamParser`, `TransactionIngestService`
- **Support**: ZIP files (with optional passwords), CSV (CAMS statements), Excel (historical support)
- **Email Validation**: `FileMetadataExtractor.extractEmailFromCsv()` ensures uploaded CSV email matches authenticated user email
- **Pattern**: Extract email from file → Validate against `users.email` → Parse → Persist with foreign key to `user_id`

---

## Project-Specific Conventions & Gotchas

### 1. **Naming Strategy: camelCase → snake_case**
Entity fields: `uploadDate` → DB column: `upload_date` (automatic via `CamelCaseToUnderscoresNamingStrategy`)

### 2. **Always Include Timestamps**
Every table has `created_at` and `updated_at` with `DEFAULT CURRENT_TIMESTAMP` and `ON UPDATE CURRENT_TIMESTAMP` (MySQL).

### 3. **Lombok Reduces Boilerplate**
- `@Data` = `@Getter @Setter @ToString @EqualsAndHashCode @RequiredArgsConstructor`
- `@RequiredArgsConstructor` = constructor with all `final` fields (Spring injects these)
- `@AllArgsConstructor` + `@NoArgsConstructor` for DTOs

### 4. **MapStruct Auto-Generates Entity ↔ DTO Mapping**
Annotated interfaces in `application/mapper/` get implementation auto-generated at build time. Speeds up controller/service layers.

### 5. **JWT Token Always from Header, Never Request Body**
Controller extracts from `@RequestHeader(HttpHeaders.AUTHORIZATION)` using `jwtTokenService.extractUserId()`. This prevents request spoofing of user ID.

### 6. **Flyway Migration Ordering**
- `V1__.sql` → `V2__.sql` → `V3__.sql` (numeric order)
- `baseline-on-migrate: true` → First run creates baseline version 1
- If migration fails: Fix SQL, increment version number, retry
- Never modify executed migration files

### 7. **Scheduled Jobs Need @EnableScheduling**
Main class has `@SpringBootApplication` (already includes scheduling); use `@Scheduled(cron = "...")` on service methods.

### 8. **Test Database Separate from Dev**
- Dev: MySQL (requires local setup + env vars)
- Tests: Can use H2 in-memory; configure in `application-test.yml` if needed
- Test classes use `@SpringBootTest` or `@DataJpaTest` for slice testing

### 9. **Response Codes Matter**
- `200 OK` - Success
- `201 Created` - Resource created
- `204 No Content` - Success, no body
- `400 Bad Request` - Invalid input (validation failure)
- `401 Unauthorized` - Missing/invalid JWT
- `403 Forbidden` - Valid JWT but not authorized for this resource
- `404 Not Found` - Resource doesn't exist
- `500 Internal Server Error` - Unexpected error

### 10. **Cross-Module Communication Pattern**
Services call other services (not controllers). Controllers are thin wrappers around services. Example: `TransactionIngestService` calls `AmfiService` to look up fund codes.

---

## Debugging & Troubleshooting

| Problem | Solution |
|---------|----------|
| **Flyway migration fails on startup** | Check `src/main/resources/db/migration/` SQL syntax; MySQL reserved keywords; ensure table names match entity names (snake_case) |
| **Hibernate tries to create tables** | Verify `hibernate.ddl-auto: none` in `application.yml`; Flyway should handle all schema |
| **JWT validation fails (401)** | Check `JWT_SECRET` env var is set; token expiration time; Bearer token format in header |
| **Build succeeds but bean creation fails** | Missing `@Component`, `@Service`, `@Repository`, or `@Configuration` on class; check Spring logs for "No bean found" errors |
| **MapStruct mapper not generated** | Ensure annotation processor runs: check `build.gradle.kts` has `annotationProcessor("org.mapstruct:mapstruct-processor:...")` |
| **Email validation fails on upload** | File encoding (check BOM); email case sensitivity (converter is case-insensitive); file delimiter (support comma, semicolon, pipe) |
| **Angular component doesn't render** | Check route in `app.routes.ts`; ensure component is declared; `authGuard` blocking unauthenticated access; check console for JavaScript errors |
| **CORS errors in browser** | Backend hasn't enabled CORS; check `application.yml` or create `@Configuration` with `@CrossOrigin` annotations |

---

## Quick Example: Adding "Export Portfolio as CSV" Feature

### Backend
1. Create DTO: `PortfolioExportDto` (in `api/dto/`)
2. Add service method: `PortfolioService.exportPortfolioAsCSV(Long userId): String`
3. Add controller endpoint: `@GetMapping("/export")` returns `ResponseEntity<byte[]>` with CSV content-type
4. No DB change needed (reads from existing entities)

### Frontend
1. Add method to `PortfolioService`: `exportPortfolio(): Observable<Blob>` calls GET with `responseType: 'blob'`
2. Add button in component template
3. Add click handler: Download blob as file using `saveAs()` from `file-saver` library (or native Blob API)

### Testing
- Unit test: Mock `PortfolioService`, verify CSV format
- Integration test: Create test user, add holdings, call endpoint, verify CSV contains correct data + only user's holdings

---

## Resources & File Locations

- **API Documentation**: http://localhost:8080/swagger-ui.html (when running)
- **API Spec (code)**: See `@RestController` classes in `src/main/java/.../api/controller/`
- **Database Schema**: `src/main/resources/db/migration/V*.sql`
- **Configuration**: `src/main/resources/application.yml`
- **Frontend Build Config**: `client/angular.json`
- **Documentation Index**: `docs/DOCUMENTATION_INDEX.md`
- **Token Auth Reference**: `docs/TOKEN_AUTH_EMAIL_VALIDATION_IMPLEMENTATION.md`
- **Upload History Architecture**: `docs/UPLOAD_HISTORY_ARCHITECTURE_DIAGRAMS.md`

---

## Getting Help

- Check `docs/` folder for feature-specific guides
- Review existing controllers/services for patterns
- Search for similar features already implemented (e.g., AMFI sync for external API integration)
- Run `./gradlew.bat test` to validate changes before committing


