const express = require('express');
const path = require('path');
const app = express();
const port = 3000;

// 정적 파일 제공
app.use(express.static(path.join(__dirname)));

// 로그인 성공 및 오류 페이지 라우팅
app.get('/login/success', (req, res) => {
  res.sendFile(path.join(__dirname, 'login-success.html'));
});

app.get('/login/error', (req, res) => {
  res.sendFile(path.join(__dirname, 'login-error.html'));
});

// 기본 라우팅
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'oauth.html'));
});

app.listen(port, () => {
  console.log(`OAuth 테스트 서버가 http://localhost:${port}에서 실행 중입니다.`);
  console.log(`로그인 페이지: http://localhost:${port}`);
  console.log(`로그인 성공 페이지: http://localhost:${port}/login/success`);
  console.log(`로그인 오류 페이지: http://localhost:${port}/login/error`);
});
