# Portfolio Tracker

A small Spring Boot application for managing and ingesting AMFI mutual fund data and basic portfolio functionality. The project is a Java Spring Boot application built with Gradle and includes database migrations and tests.

## Key features
- REST endpoints for AMFI data ingestion and retrieval
- Scheduled ingestion jobs
- Database migrations (Flyway) for schema management
- Unit tests for services, controllers, and scheduled jobs

## Prerequisites
- JDK 11 or newer (JDK 17 recommended)
- Git
- Gradle wrapper (included in the repo)
- A relational database (H2, PostgreSQL, MySQL, etc.) for production/development as configured in application settings

## Getting started (Windows - cmd.exe)

1. Clone the repository:

```cmd
git clone https://github.com/sudheer-penubarthy/portfoliomanager.git
cd portfoliomanager
```

2. Build the project:

```cmd
.\gradlew.bat clean build
```

3. Run the application:

- Using Gradle:

```cmd
.\gradlew.bat bootRun
```

- Or run the generated jar (replace <version> with the actual artifact version):

```cmd
java -jar build\libs\portfoliomanager-<version>.jar
```

4. Run tests:

```cmd
.\gradlew.bat test
```

5. (Optional) Run services with Docker Compose if provided:

```cmd
docker-compose up --build
```

## Configuration
- Application configuration is located at `src/main/resources/application.yml`.
- Database migrations are in `src/main/resources/db/migration` (Flyway-style SQL files).
- To configure the datasource, set the usual Spring properties (example keys):
  - `spring.datasource.url`
  - `spring.datasource.username`
  - `spring.datasource.password`
  - `spring.profiles.active` (for selecting profile-specific settings)

Place overrides in environment variables or in a separate `application-local.yml` and activate with `spring.profiles.active=local` when needed.

## API
- OpenAPI configuration is present in the project and will expose API docs when the application runs (check `/swagger-ui.html` or `/v3/api-docs` depending on configuration).
- Consult `src/main/java/com/example/portfoliotracker/controller` for available endpoints.

## Project structure (selected)
- `src/main/java` — application source code (controllers, services, jobs, entities, repositories)
- `src/main/resources` — configuration and DB migration scripts
- `src/test/java` — unit tests
- `build.gradle.kts`, `gradlew`, `gradlew.bat` — Gradle build and wrapper

## Troubleshooting
- If the application fails to start due to DB connectivity, verify the `spring.datasource.*` settings and that the DB is reachable.
- For migration issues, check the SQL files in `src/main/resources/db/migration`.
- On Windows use `cmd.exe` or PowerShell; ensure `gradlew.bat` is used (prefixed with `.` when using PowerShell).

## Contributing
- Open an issue for significant changes or bugs.
- Create a feature branch from `master`/`main` and include tests for new behavior.
- Run `.\gradlew.bat clean build` and `.\gradlew.bat test` locally before submitting a PR.

## License
This repository includes a `LICENCE` file at the project root; update and review license details as required.

---

If you want this README expanded with examples of API requests, local DB setup (Postgres / H2), or CI/CD notes, tell me which you'd like and I will add them.
