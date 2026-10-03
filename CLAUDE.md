# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Knygnesys** is a Lithuanian book reading, rating, and sharing system. The name references "knygnesys" (book smuggler), a historical Lithuanian figure.

## Repository Structure

```
Knygnesys/
├── knygnesys-ui/       # Angular 21 frontend
├── knygnesys-app/      # Spring Boot 3.4 backend (Java 21)
├── db-migration/       # Flyway database migrations
├── postgres.yml        # Docker Compose for PostgreSQL
└── app.yml             # Docker Compose for Spring Boot app
```

## Commands

### Frontend (`knygnesys-ui/`)

```bash
npm run start          # Dev server on http://localhost:4200
npm run build          # Production build
npm run test           # Run tests (Vitest)
npm run lint           # ESLint
npm run lint:fix       # ESLint with auto-fix
npm run format         # Prettier formatting
```

### Backend (`knygnesys-app/`)

```bash
./mvnw spring-boot:run   # Run locally (port 10032)
./mvnw test              # Run tests
./mvnw clean package     # Build JAR
```

### Database

```bash
# Start PostgreSQL
docker compose -f postgres.yml up

# Run Flyway migrations (from db-migration/)
mvn clean flyway:migrate
```

### Full Local Stack

```bash
docker compose -f postgres.yml up -d   # Start DB
cd db-migration && mvn clean flyway:migrate
cd knygnesys-app && ./mvnw spring-boot:run
cd knygnesys-ui && npm install && npm run start
```

## Architecture

### Frontend (Angular 21)

- **Standalone components** throughout — no NgModules
- **State via signals** and RxJS; services injected with `inject()` function
- **Bootstrap 5.3** for styling, SCSS for component styles
- **Vitest** (not Karma/Jasmine) as the test runner
- Book search calls the external **Open Library API** with debouncing (300ms) and result caching in `BookSearchService`
- UI language is Lithuanian

Key paths: `src/app/pages/` for page components, `src/app/services/` for injectable services.

### Backend (Spring Boot 3.4, Java 21)

- REST API served on port **10032**
- Spring Data JPA with PostgreSQL
- `application.yaml` for local dev; `application-docker.yml` activated via the `docker` Spring profile
- Multi-stage Dockerfile: Maven build stage → Eclipse Temurin 21 JRE runtime

### Database (PostgreSQL 15)

Flyway migrations live in `db-migration/src/main/resources/db/migration/`.

Core schema tables: `users`, `books`, `reading_list`, `reading_progress`, `ratings`, `goals`.

Connection: `localhost:5432`, database/user `knygnesys_db`, password `knygnesys_pw`.

### Docker Networking

`postgres.yml` creates the `knygnesys_default` network. `app.yml` must be started after `postgres.yml` so the backend can reach the `database` hostname.

## Code Style

- **ESLint + Prettier** enforced in the frontend. Prettier config: 100-char line width, single quotes.
- Angular component selector prefix: `app`.
- Stylesheet format: SCSS.