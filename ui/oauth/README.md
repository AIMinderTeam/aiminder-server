# OAuth2 로그인 UI

이 프로젝트는 간단한 OAuth2 로그인 테스트를 위한 UI 구현을 제공합니다.

## 기능

- Google OAuth2 로그인
- Kakao OAuth2 로그인
- 로그인 성공 페이지
- 로그인 실패 페이지
- 로그인 토큰 저장 및 관리

## 설치 방법

```bash
# 의존성 설치
npm install

# 개발 서버 실행
npm run dev

# 프로덕션 서버 실행
npm start
```

## 페이지 구성

- **메인 페이지 (`/`)**: OAuth2 로그인 버튼 제공
- **로그인 성공 페이지 (`/login/success`)**: 로그인 성공 시 리다이렉트되는 페이지
- **로그인 오류 페이지 (`/login/error`)**: 로그인 실패 시 리다이렉트되는 페이지

## 설정

서버 URL 및 OAuth 설정은 애플리케이션의 설정 파일에서 관리됩니다. 

```yaml
# application.yaml 참고
aiminder:
  oauth:
    success-url: ${aiminder.client.url}/login/success
    error-url: ${aiminder.client.url}/login/error

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
```

## 사용 방법

1. 서버를 실행합니다: `npm run dev`
2. 브라우저에서 `http://localhost:3000`으로 접속합니다.
3. 로그인 버튼을 클릭하여 OAuth2 인증을 시작합니다.
4. 인증 후 토큰이 발급되면 성공 페이지로 리다이렉트됩니다.
