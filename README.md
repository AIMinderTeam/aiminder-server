# 🎯 AI Minder Server

AI 기반 목표 관리 및 일정 관리 시스템

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blue.svg)](https://kotlinlang.org/)
[![JDK](https://img.shields.io/badge/JDK-21-orange.svg)](https://openjdk.java.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📚 목차
- [✨ 주요 기능](#-주요-기능)
- [🛠 기술 스택](#-기술-스택)
- [🚀 빠른 시작](#-빠른-시작)
- [📖 설치 및 실행](#-설치-및-실행)
- [🏗 프로젝트 구조](#-프로젝트-구조)
- [🔧 개발 가이드](#-개발-가이드)
- [📋 API 문서](#-api-문서)
- [🐳 Docker 배포](#-docker-배포)
- [🤝 기여하기](#-기여하기)
- [📄 라이센스](#-라이센스)

## ✨ 주요 기능

### 🤖 AI 어시스턴트
- OpenAI GPT 모델 기반 대화형 AI
- Function Calling을 통한 목표 및 일정 관리
- 개인화된 목표 달성 코칭
- 대화 기록 저장 및 컨텍스트 유지

### 🎯 스마트 목표 관리
- SMART 목표 설정 가이드
- AI 기반 목표 개선 제안
- 목표별 이미지 첨부 지원
- 목표 상태 추적 및 분석

### 📅 일정 관리
- 목표 연동 자동 일정 생성
- 주간/월간 일정 계획
- 일정 상태 관리 (준비/진행/완료/취소)
- 일정 진행률 시각화

### 🔐 안전한 인증
- OAuth2 소셜 로그인 (Google, Kakao)
- JWT 기반 토큰 인증
- 쿠키 기반 세션 관리
- 리프레시 토큰 자동 갱신

### 📁 파일 관리
- 이미지 업로드 및 검증
- 다양한 이미지 포맷 지원 (JPEG, PNG, GIF, WebP)
- 파일 크기 제한 및 보안 검증

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.5 + WebFlux
- **Language**: Kotlin 1.9.25 (JDK 21)
- **Database**: PostgreSQL 14 + R2DBC
- **AI Integration**: Spring AI 1.0.3 + OpenAI API
- **Authentication**: JWT + OAuth2
- **Migration**: Flyway
- **Code Generation**: JOOQ

### Development & DevOps
- **Build Tool**: Gradle 8.x with Kotlin DSL
- **Testing**: JUnit 5, MockK, TestContainers
- **Code Quality**: KtLint, Detekt
- **Documentation**: OpenAPI 3 + Swagger UI
- **Containerization**: Docker + Docker Compose
- **CI/CD**: GitHub Actions (설정 시)

### Architecture Patterns
- **Reactive Programming**: WebFlux + R2DBC
- **Domain-Driven Design**: 모듈별 도메인 분리
- **Clean Architecture**: 레이어별 관심사 분리
- **Configuration Management**: Profile별 환경 설정

## 🚀 빠른 시작

### 필요 조건
- JDK 21+
- PostgreSQL 14+
- Docker & Docker Compose (선택)
- Node.js 18+ (OpenAPI 클라이언트 생성용, 선택)

### 1분 만에 실행하기
```bash
# 1. 저장소 클론
git clone https://github.com/your-username/aiminder-server.git
cd aiminder-server

# 2. 환경 설정 파일 생성
cp .env.example .env
# .env 파일 편집하여 필요한 환경 변수 설정

# 3. PostgreSQL 실행 (로컬 설치 또는 Docker 사용)
# Option A: Docker 사용
docker run --name postgres -e POSTGRES_DB=aiminderdb -e POSTGRES_USER=aiminder -e POSTGRES_PASSWORD=aiminder -p 5432:5432 -d postgres:14

# Option B: 로컬 PostgreSQL 사용 (이미 설치된 경우)
createdb aiminderdb

# 4. 애플리케이션 빌드 및 실행
./gradlew bootRun

# 5. 브라우저에서 확인
open http://localhost:8080/api/swagger-ui.html
```

## 📖 설치 및 실행

### 환경 설정

#### 1. 환경 변수 설정
프로젝트 루트에 `.env` 파일을 생성하고 다음 변수들을 설정하세요:

```env
# Database
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=aiminderdb
DATABASE_USERNAME=aiminder
DATABASE_PASSWORD=your_password
DATABASE_URL=r2dbc:postgresql://localhost:5432/aiminderdb

# JWT
ACCESS_TOKEN_SECRET=your_access_token_secret_at_least_256_bits
ACCESS_TOKEN_EXPIRATION=3600000
REFRESH_TOKEN_SECRET=your_refresh_token_secret_at_least_256_bits
REFRESH_TOKEN_EXPIRATION=604800000

# Cookie
COOKIE_DOMAIN=localhost
COOKIE_SAME_SITE=lax
COOKIE_HTTP_ONLY=true
COOKIE_SECURE=false

# OpenAI
OPEN_API_KEY=your_openai_api_key

# OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

#### 2. PostgreSQL 설정

**Option A: Docker로 실행**
```bash
docker run --name postgres \
  -e POSTGRES_DB=aiminderdb \
  -e POSTGRES_USER=aiminder \
  -e POSTGRES_PASSWORD=aiminder \
  -p 5432:5432 \
  -d postgres:14
```

**Option B: 로컬 설치**
```bash
# PostgreSQL 설치 (macOS)
brew install postgresql@14
brew services start postgresql@14

# 데이터베이스 생성
createdb aiminderdb
createuser aiminder
```

### 빌드 및 실행

#### 개발 모드
```bash
# 전체 빌드
./gradlew build

# 개발 서버 실행 (Hot Reload)
./gradlew bootRun

# 특정 프로필로 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

#### 프로덕션 모드
```bash
# JAR 파일 생성
./gradlew bootJar

# JAR 파일 실행
java -jar build/libs/aiminder-server-*.jar
```

#### 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "AssistantControllerTest"

# 코드 포맷팅 확인
./gradlew ktlintCheck

# 코드 자동 포맷팅
./gradlew ktlintFormat
```

## 🏗 프로젝트 구조

```
src/main/kotlin/ai/aiminder/aiminderserver/
├── assistant/          # AI 어시스턴트 모듈
│   ├── client/        # OpenAI 클라이언트
│   ├── controller/    # REST API 컨트롤러
│   ├── service/       # 비즈니스 로직
│   └── tool/          # AI Function Tools
├── auth/              # 인증 모듈
│   ├── config/        # JWT, OAuth2 설정
│   ├── filter/        # 인증 필터
│   ├── handler/       # 로그인/로그아웃 핸들러
│   └── service/       # 토큰 관리 서비스
├── goal/              # 목표 관리 모듈
├── schedule/          # 일정 관리 모듈
├── image/             # 이미지 관리 모듈
├── user/              # 사용자 관리 모듈
├── conversation/      # 대화 기록 모듈
└── common/            # 공통 모듈
    ├── config/        # R2DBC, JOOQ 설정
    ├── error/         # 에러 처리
    └── util/          # 유틸리티
```

### 주요 설정 파일
- `application.yaml`: 기본 설정
- `application-local.yaml`: 로컬 개발 설정
- `application-openai.yaml`: OpenAI 전용 설정
- `CLAUDE.md`: Claude Code 작업 가이드
- `.env.example`: 환경 변수 템플릿

### 데이터베이스 마이그레이션
- `src/main/resources/db/migration/`: Flyway 마이그레이션 파일
- `src/main/resources/db/jooq/`: JOOQ 스키마 파일

## 📋 API 문서

### Swagger UI
개발 서버 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/v3/api-docs

### TypeScript 클라이언트
프로젝트는 OpenAPI 스펙으로부터 TypeScript 클라이언트를 자동 생성합니다.

```bash
# TypeScript 클라이언트 생성 및 NPM 배포
./openapi-generate.sh -version 1.0.0 -password your_github_token

# 생성된 클라이언트 사용
npm install @leesm0518/aiminder-api
```

### 주요 API 엔드포인트

#### 인증
- `POST /auth/login` - 로그인
- `POST /auth/logout` - 로그아웃
- `POST /auth/refresh` - 토큰 갱신

#### AI 어시스턴트
- `POST /api/v1/assistant/chat` - AI 대화
- `PUT /api/v1/assistant/conversation` - 대화 업데이트

#### 목표 관리
- `GET /api/v1/goals` - 목표 목록 조회
- `POST /api/v1/goals` - 목표 생성
- `PUT /api/v1/goals/{id}` - 목표 수정
- `DELETE /api/v1/goals/{id}` - 목표 삭제

#### 일정 관리
- `GET /api/v1/schedules` - 일정 목록 조회
- `POST /api/v1/schedules` - 일정 생성
- `PUT /api/v1/schedules/{id}` - 일정 수정
- `DELETE /api/v1/schedules/{id}` - 일정 삭제

#### 이미지 관리
- `POST /api/v1/images` - 이미지 업로드
- `GET /api/v1/images/{id}` - 이미지 조회

### API 테스트
프로젝트에는 HTTP 파일을 사용한 API 테스트가 포함되어 있습니다.

```
src/test/api/test.http
```

IntelliJ IDEA에서 직접 실행하거나 REST Client 플러그인을 사용하세요.

## 🔧 개발 가이드

### 코드 스타일
프로젝트는 KtLint를 사용하여 코드 스타일을 관리합니다.

```bash
# 코드 스타일 검사
./gradlew ktlintCheck

# 코드 자동 포맷팅
./gradlew ktlintFormat
```

### 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "AssistantControllerTest"

# 특정 테스트 메서드 실행
./gradlew test --tests "AssistantControllerTest.shouldReturnResponse"

# 통합 테스트 (TestContainers 사용)
./gradlew integrationTest
```

### 데이터베이스 마이그레이션
```bash
# Flyway 마이그레이션 실행
./gradlew flywayMigrate

# 마이그레이션 정보 확인
./gradlew flywayInfo
```

### JOOQ 코드 생성
```bash
# JOOQ 클래스 생성
./gradlew jooqCodegenMain
```

### 개발 워크플로우
1. **브랜치 생성**: `git checkout -b feature/your-feature`
2. **개발**: 코드 작성 및 테스트
3. **포맷팅**: `./gradlew ktlintFormat`
4. **테스트**: `./gradlew test`
5. **빌드**: `./gradlew build`
6. **커밋**: `git commit -m "feat: your feature"`
7. **푸시**: `git push origin feature/your-feature`
8. **PR 생성**: GitHub에서 Pull Request 생성

### 환경별 실행
```bash
# 로컬 환경
./gradlew bootRun --args='--spring.profiles.active=local'

# OpenAI 환경
./gradlew bootRun --args='--spring.profiles.active=openai'

# 개발 환경
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## 🐳 Docker 배포

### 개별 컨테이너 빌드 및 실행
```bash
# JAR 파일 생성 및 Docker 이미지 빌드
./gradlew bootJar
cp build/libs/*.jar docker/
cd docker
docker build -t aiminder-server .

# 데이터베이스 컨테이너 실행
docker run --name aiminder-database \
  -e POSTGRES_DB=aiminderdb \
  -e POSTGRES_USER=aiminder \
  -e POSTGRES_PASSWORD=aiminder \
  -p 5432:5432 \
  -d postgres:14

# 애플리케이션 컨테이너 실행
docker run -d \
  --name aiminder-server \
  --link aiminder-database \
  -p 8080:8080 \
  --env-file ../.env \
  -e DATABASE_URL=r2dbc:postgresql://aiminder-database:5432/aiminderdb \
  aiminder-server
```

### SSL 환경 배포
프로젝트에는 SSL 인증서와 함께 배포할 수 있는 설정이 포함되어 있습니다.

```bash
# SSL 인증서와 함께 배포
cd docker
docker-compose -f docker-compose-ssl.yml up -d

# Let's Encrypt 인증서 갱신
./scripts/renew-cert.sh
```

### 환경 변수 설정
Docker 환경에서는 다음 환경 변수들을 설정해야 합니다:

```bash
# docker/.env 파일 예시
DEVELOP_AIMINDER_SERVER_TAG=latest
DEVELOP_AIMINDER_CLIENT_TAG=latest
DATABASE_URL=r2dbc:postgresql://aiminder-database:5432/aiminderdb
SPRING_PROFILES_ACTIVE=dev,openai
```

### 헬스 체크
```bash
# 애플리케이션 상태 확인
curl http://localhost:8080/actuator/health

# 컨테이너 로그 확인
docker logs aiminder-server

# 데이터베이스 연결 확인
docker exec -it aiminder-database psql -U aiminder -d aiminderdb
```

### 볼륨 관리
```bash
# 데이터 백업
docker exec aiminder-database pg_dump -U aiminder aiminderdb > backup.sql

# 업로드 이미지 백업
tar -czf uploads-backup.tar.gz docker/uploads/
```
