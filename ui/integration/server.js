const express = require('express');
const path = require('path');
const app = express();
const port = 3000;

// 정적 파일 제공 (우선순위 높음)
app.use(express.static(path.join(__dirname), {
  setHeaders: (res, path) => {
    if (path.endsWith('.js')) {
      res.setHeader('Content-Type', 'application/javascript');
    } else if (path.endsWith('.css')) {
      res.setHeader('Content-Type', 'text/css');
    }
  }
}));

// 메인 라우팅 - index.html 서빙
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

// SPA 라우팅 지원 - 확장자가 없는 경로만 index.html로 서빙
app.get(/^\/(?!.*\.).*$/, (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(port, () => {
  console.log(`Integration UI 서버가 http://localhost:${port}에서 실행 중입니다.`);
  console.log(`메인 페이지: http://localhost:${port}`);
});
