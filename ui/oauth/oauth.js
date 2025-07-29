document.addEventListener('DOMContentLoaded', function() {
  // URL에서 토큰과 결과 파라미터 확인
  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get('token');
  const error = urlParams.get('error');

  const loginResultDiv = document.getElementById('login-result');
  const tokenInfoDiv = document.getElementById('token-info');
  const logoutBtn = document.getElementById('logout-btn');

  // 토큰이 있으면 로그인 성공
  if (token) {
    // 토큰을 로컬 스토리지에 저장
    localStorage.setItem('auth_token', token);

    // 토큰 정보 표시
    tokenInfoDiv.textContent = `인증 토큰: ${token.substring(0, 15)}...`;

    // 결과 컨테이너 표시
    loginResultDiv.classList.remove('hidden');

    // URL에서 토큰 파라미터 제거 (보안 목적)
    window.history.replaceState({}, document.title, window.location.pathname);

    // 사용자 정보 가져오기 (선택적)
    fetchUserInfo(token);
  }

  // 오류가 있으면 표시
  if (error) {
    tokenInfoDiv.textContent = `로그인 오류: ${error}`;
    loginResultDiv.classList.remove('hidden');
    window.history.replaceState({}, document.title, window.location.pathname);
  }

  // 이미 저장된 토큰이 있는지 확인
  const savedToken = localStorage.getItem('auth_token');
  if (savedToken && !token) {
    tokenInfoDiv.textContent = `저장된 인증 토큰: ${savedToken.substring(0, 15)}...`;
    loginResultDiv.classList.remove('hidden');

    // 사용자 정보 가져오기 (선택적)
    fetchUserInfo(savedToken);
  }

  // 로그아웃 버튼 이벤트 처리
  logoutBtn.addEventListener('click', function() {
    localStorage.removeItem('auth_token');
    loginResultDiv.classList.add('hidden');
    tokenInfoDiv.textContent = '';
  });
});

/**
 * 사용자 정보를 가져오는 함수
 * @param {string} token - 인증 토큰
 */
async function fetchUserInfo(token) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/user', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (response.ok) {
      const userData = await response.json();
      const tokenInfoDiv = document.getElementById('token-info');

      // 사용자 정보 표시
      tokenInfoDiv.innerHTML = `
        <p><strong>인증 토큰:</strong> ${token.substring(0, 15)}...</p>
        <p><strong>사용자 ID:</strong> ${userData.id || '정보 없음'}</p>
        <p><strong>로그인 제공자:</strong> ${userData.provider || '정보 없음'}</p>
      `;
    }
  } catch (error) {
    console.error('사용자 정보를 가져오는데 실패했습니다:', error);
  }
}
