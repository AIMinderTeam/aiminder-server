/**
 * Authentication module for OAuth integration
 */

class AuthManager {
    constructor() {
        this.user = null;
        this.isAuthenticated = false;
        this.sessionCheckInterval = null;
        this.sessionCheckIntervalMs = 5 * 60 * 1000; // 5 minutes
        
        this.init();
    }

    init() {
        this.bindEvents();
        this.setupPeriodicSessionCheck();
    }

    bindEvents() {
        // Login buttons
        const loginGoogle = document.getElementById('loginGoogle');
        const loginKakao = document.getElementById('loginKakao');
        const checkSessionBtn = document.getElementById('checkSessionBtn');
        const logoutBtn = document.getElementById('logoutBtn');

        if (loginGoogle) {
            loginGoogle.addEventListener('click', () => this.login('google'));
        }

        if (loginKakao) {
            loginKakao.addEventListener('click', () => this.login('kakao'));
        }

        if (checkSessionBtn) {
            checkSessionBtn.addEventListener('click', () => this.checkSession(true));
        }

        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.logout());
        }

        // Listen for authentication events
        eventBus.on('authRequired', () => {
            this.showLoginScreen();
        });

        eventBus.on('userDataUpdated', (userData) => {
            this.updateUserDisplay(userData);
        });
    }

    /**
     * Generate OAuth login URL
     */
    buildOAuthUrl(provider, returnPath = '/ui/integration/') {
        const absoluteReturn = new URL(returnPath, window.location.origin).toString();
        const returnTo = encodeURIComponent(absoluteReturn);
        return `${CONFIG.API_BASE_URL}/oauth2/authorization/${provider}?return_to=${returnTo}`;
    }

    /**
     * Initiate OAuth login
     */
    login(provider) {
        if (!['google', 'kakao'].includes(provider)) {
            ErrorHandler.handle('지원하지 않는 로그인 제공자입니다.', 'AuthManager.login');
            return;
        }

        try {
            const loginUrl = this.buildOAuthUrl(provider);
            
            // Show loading state
            this.setLoginLoading(true);
            
            // Redirect to OAuth provider
            window.location.href = loginUrl;
        } catch (error) {
            this.setLoginLoading(false);
            ErrorHandler.handle(error, 'AuthManager.login');
        }
    }

    /**
     * Check current session status
     */
    async checkSession(showStatus = false) {
        try {
            const response = await fetch(`${CONFIG.API_BASE_URL}/api/auth/me`, {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'Accept': 'application/json',
                    'Cache-Control': 'no-cache'
                }
            });

            if (response.ok) {
                const user = await response.json();
                this.handleAuthSuccess(user);
                
                if (showStatus) {
                    this.showLoginStatus('✅ 로그인 상태입니다.', 'success');
                }
                
                return true;
            } else {
                this.handleAuthFailure();
                
                if (showStatus) {
                    this.showLoginStatus('❌ 로그인되지 않았습니다.', 'error');
                }
                
                return false;
            }
        } catch (error) {
            console.error('Session check failed:', error);
            this.handleAuthFailure();
            
            if (showStatus) {
                this.showLoginStatus('⚠️ 세션 확인 중 오류가 발생했습니다.', 'error');
            }
            
            return false;
        }
    }

    /**
     * Logout user
     */
    async logout() {
        try {
            // Show loading state
            this.setLogoutLoading(true);

            // Call logout endpoint
            const response = await fetch(`${CONFIG.API_BASE_URL}/api/auth/logout`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            // Clear local user state regardless of server response
            this.handleAuthFailure();
            
            // Clear user-specific data
            this.clearUserData();
            
            // Emit logout event
            eventBus.emit('userLoggedOut');
            
            // Show login screen
            this.showLoginScreen();
            
            // Show success message
            this.showLoginStatus('✅ 로그아웃되었습니다.', 'success');
            
        } catch (error) {
            console.warn('Logout request failed (this is often expected):', error);
            
            // Still clear local state even if server request fails
            this.handleAuthFailure();
            this.clearUserData();
            eventBus.emit('userLoggedOut');
            this.showLoginScreen();
        } finally {
            this.setLogoutLoading(false);
        }
    }

    /**
     * Handle successful authentication
     */
    handleAuthSuccess(user) {
        this.user = user;
        this.isAuthenticated = true;
        
        // Update user display
        this.updateUserDisplay(user);
        
        // Emit authentication event
        eventBus.emit('userAuthenticated', user);
        
        // Show chat screen
        this.showChatScreen();
        
        console.log('User authenticated:', user);
    }

    /**
     * Handle authentication failure
     */
    handleAuthFailure() {
        this.user = null;
        this.isAuthenticated = false;
        
        // Emit unauthenticated event
        eventBus.emit('userUnauthenticated');
        
        // Show login screen
        this.showLoginScreen();
    }

    /**
     * Update user display in the UI
     */
    updateUserDisplay(user) {
        const userName = document.getElementById('userName');
        const userEmail = document.getElementById('userEmail');
        const userAvatar = document.getElementById('userAvatar');

        if (userName && user.name) {
            userName.textContent = user.name;
        }

        if (userEmail) {
            userEmail.textContent = user.email || user.id || '정보 없음';
        }

        if (userAvatar) {
            if (user.picture) {
                userAvatar.innerHTML = `<img src="${user.picture}" alt="User Avatar" style="width:100%;height:100%;border-radius:50%;object-fit:cover;">`;
            } else {
                // Use first letter of name or default icon
                const initial = user.name ? user.name.charAt(0).toUpperCase() : '👤';
                userAvatar.textContent = initial;
            }
        }
    }

    /**
     * Show login screen
     */
    async showLoginScreen() {
        const loginScreen = document.getElementById('loginScreen');
        const chatScreen = document.getElementById('chatScreen');
        const loadingSpinner = document.getElementById('loadingSpinner');

        if (loadingSpinner) {
            await Utils.hideElement(loadingSpinner);
        }

        if (chatScreen) {
            await Utils.hideElement(chatScreen);
        }

        if (loginScreen) {
            await Utils.showElement(loginScreen);
        }
    }

    /**
     * Show chat screen
     */
    async showChatScreen() {
        const loginScreen = document.getElementById('loginScreen');
        const chatScreen = document.getElementById('chatScreen');
        const loadingSpinner = document.getElementById('loadingSpinner');

        if (loadingSpinner) {
            await Utils.hideElement(loadingSpinner);
        }

        if (loginScreen) {
            await Utils.hideElement(loginScreen);
        }

        if (chatScreen) {
            await Utils.showElement(chatScreen);
        }
    }

    /**
     * Show login status message
     */
    showLoginStatus(message, type = 'info') {
        const statusContainer = document.getElementById('loginStatus');
        const statusContent = document.getElementById('statusContent');

        if (statusContainer && statusContent) {
            statusContent.innerHTML = `
                <div class="status-message status-${type}">
                    ${message}
                </div>
            `;
            statusContainer.classList.remove('hidden');

            // Auto-hide after 5 seconds
            setTimeout(() => {
                if (statusContainer) {
                    statusContainer.classList.add('hidden');
                }
            }, 5000);
        }
    }

    /**
     * Set loading state for login buttons
     */
    setLoginLoading(isLoading) {
        const loginGoogle = document.getElementById('loginGoogle');
        const loginKakao = document.getElementById('loginKakao');
        const checkSessionBtn = document.getElementById('checkSessionBtn');

        [loginGoogle, loginKakao, checkSessionBtn].forEach(btn => {
            if (btn) {
                btn.disabled = isLoading;
                if (isLoading) {
                    btn.style.opacity = '0.6';
                } else {
                    btn.style.opacity = '';
                }
            }
        });
    }

    /**
     * Set loading state for logout button
     */
    setLogoutLoading(isLoading) {
        const logoutBtn = document.getElementById('logoutBtn');
        
        if (logoutBtn) {
            logoutBtn.disabled = isLoading;
            if (isLoading) {
                logoutBtn.innerHTML = `
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                        <path d="M12 6v6l4 2" stroke="currentColor" stroke-width="2"/>
                    </svg>
                    로그아웃 중...
                `;
            } else {
                logoutBtn.innerHTML = `
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    로그아웃
                `;
            }
        }
    }

    /**
     * Clear user-specific data from local storage
     */
    clearUserData() {
        // Clear user-specific conversations
        const conversations = Storage.getItem(CONFIG.STORAGE_KEYS.CONVERSATIONS, []);
        const userPrefix = this.user ? `${this.user.id}_` : '';
        
        if (userPrefix) {
            const updatedConversations = conversations.filter(conv => 
                !conv.id.startsWith(userPrefix)
            );
            Storage.setItem(CONFIG.STORAGE_KEYS.CONVERSATIONS, updatedConversations);
        }
        
        // Keep theme and other non-user-specific preferences
    }

    /**
     * Get user-specific storage key
     */
    getUserStorageKey(key) {
        if (!this.user) return key;
        return `${this.user.id}_${key}`;
    }

    /**
     * Setup periodic session validation
     */
    setupPeriodicSessionCheck() {
        // Clear existing interval
        if (this.sessionCheckInterval) {
            clearInterval(this.sessionCheckInterval);
        }

        // Set up new interval
        this.sessionCheckInterval = setInterval(async () => {
            if (this.isAuthenticated) {
                const isValid = await this.checkSession();
                if (!isValid) {
                    console.log('Session expired, redirecting to login');
                    eventBus.emit('sessionExpired');
                }
            }
        }, this.sessionCheckIntervalMs);
    }

    /**
     * Start the authentication flow
     */
    async start() {
        // Show loading screen
        const loadingSpinner = document.getElementById('loadingSpinner');
        if (loadingSpinner) {
            loadingSpinner.classList.remove('hidden');
        }

        // Check for existing session
        const isAuthenticated = await this.checkSession();
        
        if (!isAuthenticated) {
            this.showLoginScreen();
        }
    }

    /**
     * Get current user
     */
    getCurrentUser() {
        return this.user;
    }

    /**
     * Check if user is authenticated
     */
    isUserAuthenticated() {
        return this.isAuthenticated;
    }

    /**
     * Get authentication headers for API requests
     */
    getAuthHeaders() {
        return {
            'Content-Type': 'application/json',
            // Cookie-based auth, no additional headers needed
        };
    }

    /**
     * Make authenticated API request
     */
    async makeAuthenticatedRequest(url, options = {}) {
        const defaultOptions = {
            credentials: 'include',
            headers: {
                ...this.getAuthHeaders(),
                ...(options.headers || {})
            }
        };

        const mergedOptions = {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...(options.headers || {})
            }
        };

        try {
            const response = await fetch(url, mergedOptions);
            
            // Check if session is still valid
            if (response.status === 401) {
                this.handleAuthFailure();
                throw new Error('인증이 필요합니다. 다시 로그인해주세요.');
            }
            
            return response;
        } catch (error) {
            if (error.message.includes('인증이 필요합니다')) {
                throw error;
            }
            throw new Error('네트워크 오류가 발생했습니다.');
        }
    }

    /**
     * Cleanup resources
     */
    destroy() {
        if (this.sessionCheckInterval) {
            clearInterval(this.sessionCheckInterval);
            this.sessionCheckInterval = null;
        }
    }
}

// Initialize auth manager
const authManager = new AuthManager();

// Export for global access
window.authManager = authManager;