const express = require('express');
const path = require('path');
const app = express();
const port = 3000;

// 정적 파일 제공
app.use(express.static(path.join(__dirname)));

// 모든 요청을 index.html로 라우팅하여 SPA 방식으로 작동
app.get('*', (req, res) => {
  res.sendFile(path.join(__dirname, 'oauth.html'));
});

app.listen(port, () => {
  console.log(`OAuth 테스트 서버가 http://localhost:${port}에서 실행 중입니다.`);
});
