# OAuth 테스트 UI

이 프로젝트는 OAuth 인증 테스트를 위한 UI를 제공합니다.

## 설치 및 실행

### 필요 조건

- Node.js (v14 이상)
- npm (v6 이상)

### 설치

```bash
npm install
```

### 실행

```bash
npm start
```

서버는 http://localhost:3000 에서 실행됩니다.

## 기능

- Google, Kakao OAuth 로그인
- 사용자 정보 조회
- JWT 토큰 검증
- API 테스트

## 라우팅

- `/`: 메인 페이지
- `/login/success`: 로그인 성공 후 리다이렉트 페이지
