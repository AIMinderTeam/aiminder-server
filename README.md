# AIMinder Server

AI 기반 목표 관리 및 스케줄 어시스턴트 백엔드 서버입니다. SMART 목표 설정을 지원하고, AI 피드백을 통해 사용자의 목표 달성을 돕습니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Framework** | Spring Boot 3.5.3, WebFlux (Reactive) |
| **Language** | Kotlin 1.9.25, Coroutines |
| **Database** | PostgreSQL 14, R2DBC |
| **Query Builder** | jOOQ 3.19.4 |
| **AI** | Spring AI 1.0.3, OpenAI |
| **Authentication** | JWT, OAuth2 (Google, Kakao) |
| **Migration** | Flyway 11.10.4 |
| **Documentation** | SpringDoc OpenAPI 2.8.13 |
| **Testing** | JUnit 5, MockK, TestContainers |
| **Build** | Gradle (Kotlin DSL), Java 21 |

## 주요 기능

### AI 어시스턴트
- **SMART 목표 설정**: AI가 사용자의 목표를 구체적이고 측정 가능하게 개선
- **스케줄 생성**: 목표에 맞는 실행 계획 자동 생성
- **AI 피드백**: 주기적인 진행 상황 피드백 제공

### 사용자 관리
- **OAuth2 로그인**: Google, Kakao 소셜 로그인
- **JWT 인증**: Cookie 기반 세션 관리 및 토큰 갱신
- **알림 설정**: AI 피드백 수신 시간 설정

### 목표 및 스케줄
- **목표 관리**: 생성, 조회, 수정, 삭제 및 상태 관리 (READY, INPROGRESS, COMPLETED)
- **스케줄 관리**: 목표에 연결된 일정 관리 및 완료 처리
- **이미지 첨부**: 목표에 이미지 업로드 지원

### 기타
- **알림 시스템**: 이벤트 기반 알림 발행 및 조회
- **문의 관리**: 사용자 피드백 및 문의 접수

## 프로젝트 구조

```
src/main/kotlin/ai/aiminder/aiminderserver/
├── assistant/       # AI 어시스턴트 (챗봇, 피드백 스케줄러)
├── auth/            # 인증 (OAuth2, JWT)
├── goal/            # 목표 관리
├── schedule/        # 스케줄 관리
├── conversation/    # 대화 이력 관리
├── notification/    # 알림 시스템
├── user/            # 사용자 프로필 및 설정
├── image/           # 이미지 업로드
├── inquiry/         # 문의 관리
└── common/          # 공통 유틸리티, 설정, 에러 처리
```

## 시작하기

### 요구 사항

- Java 21
- PostgreSQL 14+
- Docker (테스트 및 배포용)

### 환경 설정

1. 저장소 클론
```bash
git clone https://github.com/your-username/aiminder-server.git
cd aiminder-server
```

2. 환경 변수 설정
```bash
cp .env.example src/main/resources/.env
```

`.env` 파일을 열어 필요한 값을 설정합니다:

```properties
# Database Configuration
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=aiminderdb
DATABASE_USERNAME=aiminder
DATABASE_PASSWORD=your_password
DATABASE_URL=r2dbc:postgresql://localhost:5432/aiminderdb

# JWT Configuration (openssl rand -base64 64 로 생성)
ACCESS_TOKEN_SECRET=your_access_token_secret_at_least_256_bits
ACCESS_TOKEN_EXPIRATION=3600000
REFRESH_TOKEN_SECRET=your_refresh_token_secret_at_least_256_bits
REFRESH_TOKEN_EXPIRATION=604800000

# Cookie Configuration
COOKIE_DOMAIN=localhost
COOKIE_SAME_SITE=lax
COOKIE_HTTP_ONLY=true
COOKIE_SECURE=false

# OpenAI Configuration
OPEN_API_KEY=your_openai_api_key

# OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

3. PostgreSQL 데이터베이스 실행
```bash
docker run -d \
  --name aiminder-database \
  -e POSTGRES_DB=aiminderdb \
  -e POSTGRES_USER=aiminder \
  -e POSTGRES_PASSWORD=aiminder \
  -p 5432:5432 \
  postgres:14
```

4. 애플리케이션 실행
```bash
./gradlew bootRun
```

## 빌드 및 테스트

### 빌드

```bash
# 전체 빌드 (jOOQ 코드 생성 포함)
./gradlew build

# JAR 파일만 빌드
./gradlew bootJar
```

### 테스트

```bash
# 전체 테스트 실행 (Docker 필요)
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "ClassName"

# 특정 테스트 메서드 실행
./gradlew test --tests "ClassName.methodName"
```

### 코드 스타일

```bash
# 코드 스타일 검사
./gradlew ktlintCheck

# 자동 포맷팅
./gradlew ktlintFormat
```

### jOOQ 코드 생성

```bash
./gradlew jooqCodegenMain
```

## API 문서

로컬 환경에서 서버 실행 후 Swagger UI를 통해 API 문서를 확인할 수 있습니다:

- Swagger UI: http://localhost:8080/api/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api/v3/api-docs
- OpenAPI YAML: http://localhost:8080/api/v3/api-docs.yaml

### TypeScript 클라이언트 생성

```bash
./openapi-generate.sh -version <version> -password <github_token>
```

## Docker 배포

### Docker Compose로 실행

```bash
cd docker
docker-compose -f docker-compose-ssl.yml up -d
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY *.jar app.jar
CMD ["java", "-jar", "app.jar"]
```

## 설정 프로필

| 프로필 | 설명 |
|--------|------|
| `local` | 로컬 개발 환경 |
| `dev` | 개발 서버 환경 |
| `openai` | OpenAI 모델 사용 |
| `ollama` | Ollama 로컬 모델 사용 |

기본 프로필: `openai, local`

```bash
# 프로필 지정 실행
SPRING_PROFILES_ACTIVE=dev,openai ./gradlew bootRun
```

## 데이터베이스 마이그레이션

Flyway를 사용하여 스키마 버전을 관리합니다.

- 마이그레이션 파일 위치: `src/main/resources/db/migration/`
- 현재 버전: V16 (user_withdrawals 테이블)

마이그레이션은 애플리케이션 시작 시 자동으로 실행됩니다.

## 라이선스

이 프로젝트는 비공개 프로젝트입니다. 모든 권리는 AIMinder에 있습니다.
