const express = require('express');
const path = require('path');
const app = express();
const port = 3000;

// 정적 파일 제공
app.use(express.static(path.join(__dirname)));

// 메인 라우팅 - index.html 서빙
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

// SPA 라우팅 지원 - 모든 경로에서 index.html 서빙
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(port, () => {
  console.log(`Integration UI 서버가 http://localhost:${port}에서 실행 중입니다.`);
  console.log(`메인 페이지: http://localhost:${port}`);
});
