# AI Minder Server

AI-powered goal coaching service built with Spring Boot WebFlux and Kotlin.

## Features

- 🤖 AI-powered goal coaching using OpenAI integration
- 🔐 OAuth2 authentication (Google, Kakao)
- 📊 Goal and schedule management
- ⚡ Reactive programming with WebFlux and R2DBC
- 🐳 Docker deployment with SSL support

## Quick Start

### Prerequisites

- Java 21
- PostgreSQL 14+
- Docker & Docker Compose (for deployment)

### Environment Setup

Create a `.env` file:

```bash
# Database
DATABASE_URL=r2dbc:postgresql://localhost:5432/aiminderdb

# JWT
ACCESS_TOKEN_SECRET=your-access-token-secret
ACCESS_TOKEN_EXPIRATION=3600000
REFRESH_TOKEN_SECRET=your-refresh-token-secret  
REFRESH_TOKEN_EXPIRATION=86400000

# OAuth2
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret

# OpenAI
OPEN_API_KEY=your-openai-api-key

# Cookie Configuration
COOKIE_DOMAIN=localhost
COOKIE_SAME_SITE=lax
COOKIE_HTTP_ONLY=true
COOKIE_SECURE=false
```

### Development

```bash
# Build and test
./gradlew build

# Run server (requires PostgreSQL)
./gradlew bootRun

# Run tests
./gradlew test

# Lint and format
./gradlew ktlintCheck
./gradlew ktlintFormat
```

### OAuth UI Setup

```bash
cd ui/oauth
npm install
npm run dev  # Development server
npm start    # Production server
```

## Deployment

### Docker (SSL-enabled)

```bash
# Initialize SSL certificates
docker-compose -f docker/docker-compose-certbot-init.yml up

# Deploy full stack with SSL
docker-compose -f docker/docker-compose-ssl.yml up -d
```

### Production Stack

- **aiminder-server**: Spring Boot application
- **aiminder-database**: PostgreSQL 14 
- **aiminder-client**: Nginx reverse proxy with SSL
- **certbot**: Automated certificate management

## Architecture

### Core Technologies

- **Backend**: Spring Boot 3.5 + WebFlux + Kotlin
- **Database**: PostgreSQL with R2DBC (reactive)
- **AI**: Spring AI framework with OpenAI
- **Authentication**: JWT + OAuth2 (Google, Kakao)
- **Testing**: MockK, Reactor Test
- **Build**: Gradle with Kotlin DSL

### Project Structure

```
src/main/kotlin/ai/aiminder/
├── assistant/          # AI chat functionality
├── auth/              # OAuth2 + JWT authentication  
├── common/            # Shared utilities
└── goal/              # Goal management (coming soon)

src/main/resources/
├── db/migration/      # Flyway database migrations
├── prompts/           # AI prompt templates
└── application*.yaml  # Configuration profiles
```

### Key Features

**Assistant Module:**
- OpenAI integration with tool calling
- Conversation state management
- Goal management functions

**Authentication:**
- Cookie-based JWT sessions
- OAuth2 providers (Google, Kakao)
- Refresh token rotation

**Database:**
- Reactive R2DBC operations
- Flyway migrations
- Entities: User, RefreshToken, Goal, Schedule

## API Documentation

Generate and publish TypeScript client:

```bash
./openapi-generate.sh -version 0.0.1 -password <github_token>
```

Published to: `@leesm0518/aiminder-api`

## Testing

```bash
# Unit tests
./gradlew test

# API testing
# See src/test/api/*.http files
```

## Configuration Profiles

- `application.yaml` - Base configuration
- `application-dev.yaml` - Development
- `application-local.yaml` - Local development
- `application-openai.yaml` - OpenAI settings
- `application-ollama.yaml` - Alternative AI provider

## License

Copyright (c) 2024 Sangmin Lee (@leesm0518). All rights reserved.