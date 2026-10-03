# Backend – Knygnesys App

This file provides guidance specific to the `knygnesys-app/` Spring Boot backend.

## Tech Stack

- **Java 21**
- **Spring Boot 3.4**
- **Spring Data JPA** with PostgreSQL 15
- **Spring Session JDBC** for session management
- **Spring Security** for endpoint protection and session-based authentication
- **springdoc-openapi** for Swagger UI
- **Lombok**
- **Maven** (wrapper: `./mvnw`)
- **Flyway** migrations live in `db-migration/`, not here

## Commands

```bash
./mvnw spring-boot:run     # Run locally on port 10032
./mvnw test                # Run tests
./mvnw clean package       # Build JAR
```

## Package Structure

Package by feature, then by layer within the feature.
Base package: `lt.prifkodas.knygnesys`

```
lt.prifkodas.knygnesys/
├── KnygnesysApplication.java
├── auth/
│   ├── controller/
│   │   ├── AuthApi.java
│   │   └── AuthController.java
│   ├── service/
│   │   └── AuthService.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── RegisterRequest.java
│       └── AuthResponse.java
├── user/
│   ├── controller/
│   │   ├── UserApi.java
│   │   └── UserController.java
│   ├── service/
│   │   └── UserService.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── User.java
├── book/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── Book.java
├── readinglist/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── ReadingList.java
├── progress/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── ReadingProgress.java
├── rating/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── Rating.java
├── goal/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── Goal.java
└── shared/
    ├── config/
    │   ├── SecurityConfig.java
    │   ├── UserDetailsServiceImpl.java
    │   ├── CorsConfig.java
    │   └── OpenApiConfig.java
    └── exception/
        ├── GlobalExceptionHandler.java
        └── ResourceNotFoundException.java
```

## Code Style

- Entities sit at the feature root, e.g. `user/User.java`, not in a sub-package
- No separate domain layer — entity class is the domain object
- Services are plain `@Service` classes — no interface, no Impl suffix
- Controllers implement an `*Api` interface that holds all Swagger annotations and request mappings
- DTOs are plain classes with getters/setters — no records
- Lombok `@Data` `@AllArgsConstructor` `@NoArgsConstructor` used for DTO and Entity classes
- Lombok `@RequiredArgsConstructor` used for autowiring spring beans
- Package naming: `{Feature}Api`, `{Feature}Controller`, `{Feature}Service`, `{Feature}Repository`

## REST Conventions

- Base path: `/api/{feature}` e.g. `/api/auth`, `/api/users`, `/api/books`
- Use standard HTTP verbs: `GET`, `POST`, `PUT`, `DELETE`
- Return `ResponseEntity<T>` from controllers
- Error responses handled centrally via `GlobalExceptionHandler`
- `IllegalArgumentException` maps to 400, `ResourceNotFoundException` maps to 404

## Auth Endpoints

- `POST /api/auth/register` — 201 on success, 400 if email/username taken
- `POST /api/auth/login`    — 200 + session cookie set automatically
- `POST /api/auth/logout`   — 204, session invalidated
- `GET  /api/users/me`      — 200 with current user info, 401 if not authenticated

## Authentication

- Spring Security handles authentication via `UserDetailsService`
- `UserDetailsServiceImpl` loads users by username from the database
- `AuthenticationManager` used in `AuthService.login()` to authenticate
- On login: Spring Security context stored in session via `HttpSessionSecurityContextRepository`
- On logout: `SecurityContextHolder` cleared, session invalidated
- Current user retrieved via `@AuthenticationPrincipal UserDetails` in controllers
- No JWT, no token storage, no password hashing — university project (`NoOpPasswordEncoder`)
- No refresh tokens
- Login uses **username** (not email)

## Security Config

- Spring Security enabled, login form disabled, HTTP basic disabled
- CSRF disabled
- `NoOpPasswordEncoder` used (plain text passwords)
- Public endpoints: `/api/auth/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**`
- All other endpoints require authentication
- Unauthenticated requests return 401 via custom `authenticationEntryPoint`
- CORS configured for `http://localhost:4200` with `allowCredentials: true`

## Session Cookie Config (application.yaml)

```yaml
server:
  servlet:
    session:
      cookie:
        same-site: lax
        http-only: true
```

## Database

- Connection: `localhost:5432`, database `knygnesys_db`, user `knygnesys_db`, password `knygnesys_pw`
- `application.yaml` for local dev, `application-docker.yml` for Docker profile
- `spring.session.jdbc.initialize-schema: never` — Flyway handles session tables
- Use Spring Data JPA methods or JPQL `@Query` — no raw SQL
- Schema changes go in `db-migration/` — never modify existing migration files

## Swagger

- UI: `http://localhost:10032/swagger-ui/index.html`
- Docs: `http://localhost:10032/api-docs`
- All Swagger annotations go in the `*Api` interface only, never in the controller
- Always publicly accessible

## Testing

- Unit tests with JUnit 5 + Mockito
- `@ExtendWith(MockitoExtension.class)` on all test classes
- Mock `UserRepository`, `AuthenticationManager`, `HttpServletRequest` — do not hit real DB
- `AuthService` login tests mock `authenticationManager.authenticate()` — Spring Security throws
  `BadCredentialsException` on wrong credentials
- `AuthService` logout tests mock `request.getSession(false)` then verify `invalidate()`
- Test files mirror main source structure under `src/test/java/`
- Test class naming: `{Class}Test`
- Test method naming: `methodName_condition_expectedResult`
- Error messages in tests must match exactly what the service throws