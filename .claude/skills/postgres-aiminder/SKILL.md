---
name: postgres-aiminder
description: |
  AiMinder 프로젝트 로컬 PostgreSQL 데이터베이스 조회 및 관리 도구.
  사용 시점: (1) 로컬 DB 데이터 조회, (2) 테이블 구조 확인, 
  (3) SQL 쿼리 실행, (4) 사용자/목표/일정 데이터 분석
---

# PostgreSQL AiMinder Database Skill

AiMinder 프로젝트의 로컬 Docker PostgreSQL 데이터베이스에 접근하고 관리하는 스킬입니다.

## 연결 정보

**기본 연결 설정** (`.env` 파일 또는 `application-local.yaml` 기준):
- Host: `localhost`
- Port: `5432`
- Database: `aiminderdb`
- User: `aiminder`
- Password: `aiminder` (로컬 개발용)

**환경변수 방식 연결**:
```bash
export DATABASE_HOST=localhost
export DATABASE_PORT=5432
export DATABASE_NAME=aiminderdb
export DATABASE_USERNAME=aiminder
export DATABASE_PASSWORD=aiminder
```

## 주요 테이블 및 용도

### 1. 사용자 관리
- `users` - 사용자 기본 정보 (OAuth2 인증)
- `refresh_token` - JWT 토큰 관리

### 2. 목표 및 일정 관리
- `goals` - 사용자 목표 관리 (AI 생성 포함)
- `schedules` - 목표별 세부 일정
- `images` - 목표에 첨부된 이미지

### 3. AI 채팅 및 대화
- `conversations` - 대화 세션 관리
- `chat` - 채팅 메시지 이력
- `spring_ai_chat_memory` - AI 모델 메모리

### 4. 알림
- `notifications` - 사용자 알림 관리

## 사용 방법

### 빠른 쿼리 실행

```bash
python .claude/skills/postgres-aiminder/scripts/query_postgres.py "SELECT * FROM users LIMIT 5"
```

### 주요 데이터 조회 예시

**사용자 목록 확인**:
```bash
python scripts/query_postgres.py "SELECT user_id, provider, provider_id, created_at FROM users"
```

**활성 목표 확인**:
```bash
python scripts/query_postgres.py "SELECT g.title, g.description, g.target_date, u.provider_id 
FROM goals g 
JOIN users u ON g.user_id = u.user_id 
WHERE g.status = 'ACTIVE' AND g.deleted_at IS NULL"
```

**최근 대화 확인**:
```bash
python scripts/query_postgres.py "SELECT c.conversation_id, u.provider_id, c.created_at 
FROM conversations c 
JOIN users u ON c.user_id = u.user_id 
ORDER BY c.created_at DESC LIMIT 10"
```

**테이블 구조 확인**:
```bash
python scripts/query_postgres.py "SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'goals' 
ORDER BY ordinal_position"
```

### JSON 형식 출력

```bash
python scripts/query_postgres.py "SELECT * FROM goals LIMIT 3" --json
```

## Docker 상태 확인

데이터베이스 연결 실패 시:

```bash
# Docker 컨테이너 상태 확인
docker ps | grep postgres

# PostgreSQL 로그 확인
docker logs postgres_container_name

# 프로젝트의 DB 컨테이너 시작 (있는 경우)
docker-compose up -d postgres
```

## 안전 주의사항

1. **읽기 전용 권장**: 기본적으로 SELECT 쿼리만 사용
2. **대용량 조회 제한**: LIMIT 사용 권장 (성능상 이유)
3. **개발 환경 전용**: 프로덕션 DB 접근 금지
4. **민감 데이터 주의**: OAuth 토큰이나 개인정보 노출 주의

## 문제 해결

### 연결 실패 시
1. Docker PostgreSQL 컨테이너가 실행 중인지 확인
2. `.env` 파일의 데이터베이스 설정 확인
3. 방화벽이나 포트 충돌 확인 (5432번 포트)

### 권한 오류 시
- 데이터베이스 사용자 권한 확인
- 다른 애플리케이션의 DB 락 확인

## 관련 파일

- 스키마 정의: `src/main/resources/db/jooq/schema.sql`
- Migration 파일: `src/main/resources/db/migration/V*.sql`
- 설정 파일: `src/main/resources/application-local.yaml`
- 환경변수 템플릿: `.env.example`