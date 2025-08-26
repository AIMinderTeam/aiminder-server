document.addEventListener('DOMContentLoaded', function() {
  const loginResultDiv = document.getElementById('login-result');
  const infoDiv = document.getElementById('token-info');
  const logoutBtn = document.getElementById('logout-btn');
  const checkSessionBtn = document.getElementById('check-session-btn');
  const API_BASE = 'http://localhost:8080';

  // OAuth URL 생성 유틸리티
  function buildOAuthUrl(provider, returnPath = '/login/success') {
    const absoluteReturn = new URL(returnPath, window.location.origin).toString();
    const returnTo = encodeURIComponent(absoluteReturn);
    return `${API_BASE}/oauth2/authorization/${provider}?return_to=${returnTo}`;
  }

  // 페이지 내 OAuth 링크에 동적 href 적용
  function applyOAuthLinks() {
    const googleLink = document.getElementById('login-google');
    const kakaoLink = document.getElementById('login-kakao');
    if (googleLink) {
      googleLink.href = buildOAuthUrl('google', '/login/success');
    }
    if (kakaoLink) {
      kakaoLink.href = buildOAuthUrl('kakao', '/login/success');
    }
  }

  // 초기 세션 상태 확인
  applyOAuthLinks();
  checkSession();

  // 이벤트 바인딩
  if (checkSessionBtn) {
    checkSessionBtn.addEventListener('click', function() {
      checkSession(true);
    });
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', async function() {
      await logout();
      // 로그아웃 후 세션 재확인
      await checkSession(true);
    });
  }

  async function checkSession(showEmpty = false) {
    try {
      const response = await fetch(`${API_BASE}/api/auth/user`, {
        method: 'GET',
        credentials: 'include',
        headers: { 'Accept': 'application/json' }
      });

      if (response.ok) {
        const user = await response.json();
        renderUser(user);
        loginResultDiv.classList.remove('hidden');
      } else {
        if (showEmpty) {
          infoDiv.textContent = '로그인되지 않았습니다. 로그인 버튼을 사용하세요.';
          loginResultDiv.classList.remove('hidden');
        } else {
          loginResultDiv.classList.add('hidden');
          infoDiv.textContent = '';
        }
      }
    } catch (e) {
      console.error('세션 확인 실패:', e);
      infoDiv.textContent = '세션 확인 중 오류가 발생했습니다.';
      loginResultDiv.classList.remove('hidden');
    }
  }

  async function logout() {
    try {
      const resp = await fetch(`${API_BASE}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include'
      });
      if (!resp.ok) {
        // 서버에 로그아웃 엔드포인트가 없을 수 있음
        console.warn('로그아웃 API 응답 상태:', resp.status);
      }
    } catch (e) {
      console.warn('로그아웃 호출 실패(무시 가능):', e);
    }
  }

  function renderUser(user) {
    const id = user.id || user.userId || '정보 없음';
    const provider = user.provider || user.providerId || '정보 없음';
    infoDiv.innerHTML = `
      <p><strong>사용자 ID:</strong> ${id}</p>
      <p><strong>로그인 제공자:</strong> ${provider}</p>`;
  }
});
