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

# Generate jOOQ classes from schema
./gradlew jooqCodegenMain

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
- **Framework**: Spring Boot 3.5.3 + WebFlux (reactive)
- **Language**: Kotlin 1.9.25 with coroutines
- **Database**: PostgreSQL 14 with R2DBC (reactive database access)
- **Query Builder**: jOOQ 3.19.4 for type-safe SQL queries
- **AI Integration**: Spring AI 1.0.3 framework with OpenAI
- **Authentication**: JWT + OAuth2 (Google, Kakao)
- **Build**: Gradle with Kotlin DSL
- **Testing**: JUnit 5, MockK, SpringMockK, TestContainers, Reactor Test
- **Java**: 21 (required)

### Package Structure

Base package: `ai.aiminder.aiminderserver`

### Key Architectural Patterns

**Reactive Programming**: Entire application uses WebFlux and R2DBC for non-blocking, reactive operations. All database operations return `Mono<T>` or `Flux<T>`.

**Modular Domain Structure**: Code organized by business domains under `src/main/kotlin/ai/aiminder/aiminderserver/`:
- `assistant/`: AI chat functionality with OpenAI integration, tool calling, and feedback scheduling
- `auth/`: OAuth2 + JWT authentication with cookie-based sessions
- `goal/`: Goal management system with SMART goal support
- `user/`: User profile, notification settings, and withdrawal management
- `image/`: Image upload and management functionality
- `schedule/`: Schedule management system linked to goals
- `notification/`: Notification management with event-driven architecture
- `conversation/`: Conversation history and memory management
- `inquiry/`: User inquiry and feedback management system
- `common/`: Shared utilities, configuration, error handling

**Configuration Management**: Environment-driven configuration using `.env` files loaded via dotenv-java, with profile-specific YAML files.

### Database & Migrations
- **Migration Tool**: Flyway 11.10.4 for database schema versioning
- **Location**: `src/main/resources/db/migration/V*__*.sql`
- **Current Version**: V16 (user_withdrawals table)
- **Entities**: User, RefreshToken, Goal, Image, Schedule, Conversation, Chat, Notification, Inquiry, UserNotificationSettings, UserWithdrawal
- **jOOQ Schema**: `src/main/resources/db/jooq/schema.sql` for code generation
- **Generated Code**: `build/generated/jooq/` (package: `ai.aiminder.aiminderserver.jooq`)
- **Testing**: Uses TestContainers for integration tests with PostgreSQL

### Authentication Flow
- Cookie-based JWT sessions with refresh token rotation
- OAuth2 providers: Google and Kakao
- Unified `AuthenticationWebFilter` handles both Bearer token and Cookie-based authentication
- Success/failure handlers for token management
- Token extraction, validation, and refresh services

### AI Integration
- **Framework**: Spring AI with OpenAI client
- **Features**: Function calling (tool calling), conversation memory (JDBC-based), goal/schedule management tools
- **AI Tools**:
  - `GoalTool`: SMART goal refinement (`refineGoal`), goal saving (`saveGoal`), schedule saving (`saveSchedules`)
  - `TodayTool`: Current date retrieval (`getToday`)
- **AI Clients**:
  - `GoalAssistantClient`: Goal-focused conversations
  - `FeedbackAssistantClient`: AI feedback generation
- **Prompts**: Managed in `src/main/resources/prompts/` (goal_prompt.txt, feedback_prompt.txt, welcome_message.txt)
- **Feedback Scheduler**: Automated feedback generation via `FeedbackScheduler`
- **Configuration**: Profile-based (openai, ollama) with environment variables

### Testing Strategy
- **Base Class**: `BaseIntegrationTest` for WebFlux integration tests (`src/test/kotlin/ai/aiminder/aiminderserver/common/`)
- **Database**: TestContainers 1.21.3 with PostgreSQL for real database testing
- **Setup**: Flyway migration + cleanup per test
- **Mocking**: MockK 1.13.12 and SpringMockK 4.0.2 for unit tests

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
- Swagger UI: springdoc-openapi-starter-webflux-ui 2.8.13

## Important Notes

- Always run `ktlintFormat` before committing (enforced by test task)
- Integration tests require Docker for TestContainers
- Environment variables must be set via `.env` file for local development
- Database container auto-started by openapi-generate.sh script when needed
- Java 21 is required (configured in build.gradle.kts)
- Uses Gradle Kotlin DSL for build configuration
- jOOQ code generation runs automatically before Kotlin compilation

### Local Development Requirements
- PostgreSQL 14+ running on localhost:5432
- `.env` file with required environment variables (see README.md for template)
- Docker for TestContainers and OpenAPI generation

### Domain-Specific Notes

**Assistant Module**: Uses Spring AI framework with function calling capabilities. The AI assistant can execute goals and schedules management through predefined tools (`GoalTool`, `TodayTool`). Includes `FeedbackScheduler` for automated AI feedback generation and event-driven notification publishing. Prompts are managed in `src/main/resources/prompts/`.

**Authentication**: Implements unified authentication support (Bearer token and Cookie-based) through `AuthenticationWebFilter`. OAuth2 integration supports Google and Kakao providers with custom success/failure handlers for token management.

**Goal Management**: Supports SMART goal creation with AI-powered refinement. Goals have status tracking (READY, INPROGRESS, COMPLETED) and can have associated images. Goals are linked to schedules for progress tracking.

**Image Handling**: Supports file uploads with validation for image types (JPEG, PNG, GIF, WebP) and size limits (5MB). Upload directory is configurable via application properties.

**Schedule Management**: Provides schedule generation linked to goals. Schedules have status tracking (READY, COMPLETED) for progress monitoring.

**Notification System**: Event-driven notification system with `NotificationEventListener`. Supports notification types (ASSISTANT_FEEDBACK). Publishes notifications via Spring application events (`CreateNotificationEvent`, `CreateFeedbackEvent`).

**Inquiry System**: Manages user inquiries and feedback with support for different types (REVIEW, BUG_REPORT, IMPROVEMENT_SUGGESTION, GENERAL). Includes status tracking (PENDING, IN_PROGRESS, RESOLVED) and optional contact email collection.

**User Management**: User profile management with notification settings (AI feedback enablement, notification timing). Supports user withdrawal with reason tracking (SERVICE_DISSATISFACTION, USING_OTHER_SERVICE, PRIVACY_CONCERN, LOW_USAGE_FREQUENCY, OTHER). Soft delete via `deleted_at` column.