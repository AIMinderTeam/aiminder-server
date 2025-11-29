# AiMinder Database Schema Reference

이 문서는 Claude가 효과적인 SQL 쿼리를 작성할 수 있도록 AiMinder 데이터베이스의 스키마 정보를 제공합니다.

## 테이블 관계도

```
users (사용자)
├── refresh_token (1:N) - JWT 토큰 관리
├── images (1:N) - 사용자가 업로드한 이미지
├── goals (1:N) - 사용자의 목표
│   ├── schedules (1:N) - 목표별 세부 일정
│   └── conversations (1:N) - 목표 관련 대화
├── conversations (1:N) - 사용자별 대화 세션
│   └── chat (1:N) - 대화 메시지
└── notifications (1:N) - 사용자 알림
```

## 테이블 상세 정보

### 1. users (사용자)
OAuth2 기반 사용자 정보 관리

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| user_id | uuid | PK, DEFAULT uuid_generate_v4() | 사용자 고유 ID |
| provider | VARCHAR(50) | NOT NULL | OAuth2 제공자 (google, kakao) |
| provider_id | VARCHAR(255) | NOT NULL | 제공자별 사용자 ID |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| deleted_at | TIMESTAMP | NULL | 삭제일시 (소프트 삭제) |

**인덱스**: `idx_users_provider_id`  
**고유 제약**: `(provider, provider_id)`

### 2. refresh_token (JWT 토큰)
사용자 인증 토큰 관리

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| refresh_token_id | uuid | PK, DEFAULT uuid_generate_v4() | 토큰 고유 ID |
| user_id | uuid | FK → users.user_id | 사용자 참조 |
| token | VARCHAR(1000) | UNIQUE, NOT NULL | 리프레시 토큰 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 수정일시 |

### 3. images (이미지)
사용자가 업로드한 이미지 파일 관리

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| image_id | uuid | PK, DEFAULT uuid_generate_v4() | 이미지 고유 ID |
| user_id | uuid | FK → users.user_id | 사용자 참조 |
| original_file_name | VARCHAR(255) | NOT NULL | 원본 파일명 |
| stored_file_name | VARCHAR(255) | NOT NULL | 저장된 파일명 |
| file_path | VARCHAR(500) | NOT NULL | 파일 경로 |
| file_size | BIGINT | NOT NULL | 파일 크기 (bytes) |
| content_type | VARCHAR(100) | NOT NULL | MIME 타입 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |
| deleted_at | TIMESTAMP | NULL | 삭제일시 |

### 4. goals (목표)
사용자의 목표 관리 (AI 생성 목표 포함)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| goal_id | uuid | PK, DEFAULT uuid_generate_v4() | 목표 고유 ID |
| user_id | uuid | FK → users.user_id | 사용자 참조 |
| title | VARCHAR(500) | NOT NULL | 목표 제목 |
| description | TEXT | NULL | 목표 설명 |
| target_date | TIMESTAMP | NOT NULL | 목표 달성 목표일 |
| is_ai_generated | BOOLEAN | DEFAULT FALSE | AI 생성 목표 여부 |
| status | VARCHAR | DEFAULT 'ACTIVE' | 목표 상태 |
| image_id | uuid | FK → images.image_id | 첨부 이미지 참조 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| deleted_at | TIMESTAMP | NULL | 삭제일시 |

**가능한 status 값**: `ACTIVE`, `COMPLETED`, `CANCELLED` 등

### 5. schedules (일정)
목표별 세부 실행 일정

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| schedule_id | uuid | PK, DEFAULT uuid_generate_v4() | 일정 고유 ID |
| goal_id | uuid | FK → goals.goal_id | 목표 참조 |
| user_id | uuid | FK → users.user_id | 사용자 참조 |
| title | VARCHAR(255) | NOT NULL | 일정 제목 |
| description | TEXT | NULL | 일정 설명 |
| status | VARCHAR(20) | DEFAULT 'READY' | 일정 상태 |
| start_date | TIMESTAMP | NOT NULL | 시작일시 |
| end_date | TIMESTAMP | NOT NULL | 종료일시 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| deleted_at | TIMESTAMP | NULL | 삭제일시 |

**제약 조건**: `CHECK (start_date <= end_date)`  
**가능한 status 값**: `READY`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

### 6. conversations (대화 세션)
사용자와 AI의 대화 세션 관리

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| conversation_id | uuid | PK, DEFAULT uuid_generate_v4() | 대화 세션 ID |
| user_id | uuid | FK → users.user_id | 사용자 참조 |
| goal_id | uuid | FK → goals.goal_id (NULL 가능) | 관련 목표 참조 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| deleted_at | TIMESTAMP | NULL | 삭제일시 |

### 7. chat (채팅 메시지)
대화 세션별 개별 메시지 이력

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| chat_id | BIGSERIAL | PK | 메시지 고유 ID |
| conversation_id | UUID | FK → conversations.conversation_id | 대화 세션 참조 |
| content | TEXT | NOT NULL | 메시지 내용 |
| type | VARCHAR(50) | NOT NULL | 메시지 타입 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 생성일시 |

**가능한 type 값**: `USER`, `ASSISTANT`, `SYSTEM` 등

### 8. spring_ai_chat_memory (AI 메모리)
Spring AI 프레임워크의 채팅 메모리 관리

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| conversation_id | VARCHAR(255) | PK (복합키) | 대화 ID |
| message_index | SERIAL | PK (복합키) | 메시지 인덱스 |
| content | VARCHAR(1000) | NOT NULL | 메시지 내용 |
| type | VARCHAR(50) | NOT NULL | 메시지 타입 |
| timestamp | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 타임스탬프 |

### 9. notifications (알림)
사용자별 알림 관리

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| notification_id | uuid | PK, DEFAULT uuid_generate_v4() | 알림 고유 ID |
| type | VARCHAR(30) | NOT NULL | 알림 타입 |
| title | VARCHAR(255) | NOT NULL | 알림 제목 |
| description | TEXT | NOT NULL | 알림 내용 |
| metadata | JSONB | NOT NULL | 추가 메타데이터 |
| checked | BOOLEAN | NOT NULL | 확인 여부 |
| receiver_id | uuid | FK → users.user_id | 수신자 참조 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| deleted_at | TIMESTAMP | NULL | 삭제일시 |

## 자주 사용하는 쿼리 패턴

### 1. 사용자별 활성 목표 조회
```sql
SELECT g.title, g.description, g.target_date, g.is_ai_generated
FROM goals g
JOIN users u ON g.user_id = u.user_id
WHERE u.provider_id = 'user@example.com'
  AND g.status = 'ACTIVE'
  AND g.deleted_at IS NULL
ORDER BY g.target_date;
```

### 2. 목표별 일정 현황
```sql
SELECT 
    g.title as goal_title,
    s.title as schedule_title,
    s.status,
    s.start_date,
    s.end_date
FROM goals g
JOIN schedules s ON g.goal_id = s.goal_id
WHERE g.goal_id = 'goal_uuid_here'
  AND s.deleted_at IS NULL
ORDER BY s.start_date;
```

### 3. 최근 대화 이력
```sql
SELECT 
    c.conversation_id,
    u.provider_id,
    g.title as related_goal,
    c.created_at
FROM conversations c
JOIN users u ON c.user_id = u.user_id
LEFT JOIN goals g ON c.goal_id = g.goal_id
ORDER BY c.created_at DESC
LIMIT 10;
```

### 4. 미확인 알림 조회
```sql
SELECT 
    n.title,
    n.description,
    n.type,
    n.created_at
FROM notifications n
JOIN users u ON n.receiver_id = u.user_id
WHERE u.provider_id = 'user@example.com'
  AND n.checked = FALSE
  AND n.deleted_at IS NULL
ORDER BY n.created_at DESC;
```

## 주의사항

1. **소프트 삭제**: 대부분의 테이블에서 `deleted_at` 컬럼을 사용한 소프트 삭제 적용
2. **UUID 사용**: 모든 PK는 UUID 사용 (보안상 이유)
3. **타임스탬프**: 모든 테이블에 `created_at`, `updated_at` 필드 존재
4. **외래키 제약**: CASCADE 삭제 정책 적용
5. **인덱스**: 자주 조회되는 컬럼에 인덱스 설정됨