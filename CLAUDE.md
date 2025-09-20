# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Commands

### Build & Development
```bash
# Build entire project 
./gradlew build

# Run server (requires PostgreSQL)
./gradlew bootRun

# Run tests with formatting
./gradlew test

# Code formatting and linting
./gradlew ktlintCheck      # Check code style
./gradlew ktlintFormat     # Auto-format code

# Generate OpenAPI docs and publish TypeScript client
./openapi-generate.sh -version <version> -password <github_token>
```

### Testing
```bash
# Run all tests (includes integration tests with TestContainers)
./gradlew test

# Run single test class
./gradlew test --tests "ClassName"

# Run single test method
./gradlew test --tests "ClassName.methodName"

# API testing via HTTP files
# See src/test/api/test.http
```

## Architecture Overview

### Technology Stack
- **Framework**: Spring Boot 3.5 + WebFlux (reactive)
- **Language**: Kotlin with coroutines
- **Database**: PostgreSQL 14 with R2DBC (reactive database access)
- **AI Integration**: Spring AI framework with OpenAI
- **Authentication**: JWT + OAuth2 (Google, Kakao)
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, MockK, TestContainers, Reactor Test

### Key Architectural Patterns

**Reactive Programming**: Entire application uses WebFlux and R2DBC for non-blocking, reactive operations. All database operations return `Mono<T>` or `Flux<T>`.

**Modular Domain Structure**: Code organized by business domains:
- `assistant/`: AI chat functionality with OpenAI integration and tool calling
- `auth/`: OAuth2 + JWT authentication with cookie-based sessions  
- `goal/`: Goal management system
- `user/`: User profile management
- `common/`: Shared utilities and configuration

**Configuration Management**: Environment-driven configuration using `.env` files loaded via dotenv-java, with profile-specific YAML files.

### Database & Migrations
- **Migration Tool**: Flyway for database schema versioning
- **Location**: `src/main/resources/db/migration/V*__*.sql`
- **Entities**: User, RefreshToken, Goal, AiGoal, AiSchedule
- **Testing**: Uses TestContainers for integration tests with PostgreSQL

### Authentication Flow
- Cookie-based JWT sessions with refresh token rotation
- OAuth2 providers: Google and Kakao
- Custom filters: `BearerTokenAuthenticationWebFilter`, `CookieAuthenticationWebFilter`
- Success/failure handlers for token management

### AI Integration
- **Framework**: Spring AI with OpenAI client
- **Features**: Function calling, conversation memory, goal management tools
- **Prompts**: Managed in `src/main/resources/prompts/`
- **Configuration**: Profile-based (openai, ollama) with environment variables

### Testing Strategy
- **Base Class**: `BaseIntegrationTest` for WebFlux integration tests
- **Database**: TestContainers with PostgreSQL for real database testing
- **Setup**: Flyway migration + cleanup per test
- **Mocking**: MockK for unit tests

### Configuration Profiles
- `application.yaml`: Base configuration
- `application-local.yaml`: Local development 
- `application-dev.yaml`: Development environment
- `application-openai.yaml`: OpenAI-specific settings
- `application-ollama.yaml`: Alternative AI provider

### OpenAPI & Client Generation
- Generates TypeScript client via Docker-based OpenAPI Generator
- Publishes to GitHub Packages as `@leesm0518/aiminder-api`
- Automated via `openapi-generate.sh` script

## Important Notes

- Always run `ktlintFormat` before committing (enforced by test task)
- Integration tests require Docker for TestContainers
- Environment variables must be set via `.env` file for local development
- Database container auto-started by openapi-generate.sh script when needed
- Java 21 is required (configured in build.gradle.kts)
- Uses Gradle Kotlin DSL for build configuration

### Local Development Requirements
- PostgreSQL 14+ running on localhost:5432
- `.env` file with required environment variables (see README.md for template)
- Docker for TestContainers and OpenAPI generation