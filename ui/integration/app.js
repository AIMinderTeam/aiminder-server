/**
 * Main application controller
 */

class App {
    constructor() {
        this.isInitialized = false;
        this.currentScreen = null;
        
        this.init();
    }

    async init() {
        try {
            console.log('🚀 AI 목표 코칭봇 애플리케이션 시작');
            
            // Initialize theme
            ThemeManager.init();
            
            // Setup global event handlers
            this.setupGlobalEvents();
            
            // Setup error modal
            this.setupErrorModal();
            
            // Setup user menu
            this.setupUserMenu();
            
            // Setup theme toggle
            this.setupThemeToggle();
            
            // Start authentication flow
            await authManager.start();
            
            this.isInitialized = true;
            console.log('✅ 애플리케이션 초기화 완료');
            
        } catch (error) {
            console.error('❌ 애플리케이션 초기화 실패:', error);
            ErrorHandler.handle(error, 'App.init');
        }
    }

    setupGlobalEvents() {
        // Listen for authentication events
        eventBus.on('userAuthenticated', (user) => {
            console.log('👤 사용자 인증됨:', user.name || user.id);
            this.onUserAuthenticated(user);
        });

        eventBus.on('userUnauthenticated', () => {
            console.log('🚪 사용자 인증 해제됨');
            this.onUserUnauthenticated();
        });

        eventBus.on('userLoggedOut', () => {
            console.log('👋 사용자 로그아웃됨');
            this.onUserLoggedOut();
        });

        eventBus.on('sessionExpired', () => {
            console.log('⏰ 세션 만료됨');
            this.onSessionExpired();
        });

        eventBus.on('themeChanged', (theme) => {
            console.log('🎨 테마 변경됨:', theme);
            this.onThemeChanged(theme);
        });

        eventBus.on('error', (errorData) => {
            console.error('❌ 애플리케이션 오류:', errorData);
            this.onError(errorData);
        });

        // Handle window events
        window.addEventListener('beforeunload', () => {
            this.cleanup();
        });

        // Handle online/offline status
        window.addEventListener('online', () => {
            this.onConnectionStatusChange(true);
        });

        window.addEventListener('offline', () => {
            this.onConnectionStatusChange(false);
        });

        // Handle visibility change
        document.addEventListener('visibilitychange', () => {
            this.onVisibilityChange();
        });

        // Handle keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            this.handleKeyboardShortcuts(e);
        });

        // Handle clicks outside dropdowns
        document.addEventListener('click', (e) => {
            this.handleOutsideClick(e);
        });
    }

    setupErrorModal() {
        const errorModal = document.getElementById('errorModal');
        const errorModalClose = document.getElementById('errorModalClose');
        const errorModalOk = document.getElementById('errorModalOk');

        if (errorModalClose) {
            errorModalClose.addEventListener('click', () => {
                ErrorHandler.hideError();
            });
        }

        if (errorModalOk) {
            errorModalOk.addEventListener('click', () => {
                ErrorHandler.hideError();
            });
        }

        if (errorModal) {
            errorModal.addEventListener('click', (e) => {
                if (e.target === errorModal) {
                    ErrorHandler.hideError();
                }
            });
        }
    }

    setupUserMenu() {
        const userBtn = document.getElementById('userBtn');
        const userDropdown = document.getElementById('userDropdown');

        if (userBtn && userDropdown) {
            userBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                userDropdown.classList.toggle('active');
            });
        }
    }

    setupThemeToggle() {
        const themeToggle = document.getElementById('themeToggle');
        
        if (themeToggle) {
            themeToggle.addEventListener('click', () => {
                const newTheme = ThemeManager.toggleTheme();
                this.updateThemeIcon(newTheme);
            });
        }

        // Set initial theme icon
        this.updateThemeIcon(ThemeManager.getTheme());
    }

    updateThemeIcon(theme) {
        const themeToggle = document.getElementById('themeToggle');
        if (!themeToggle) return;

        if (theme === CONFIG.THEME.DARK) {
            themeToggle.innerHTML = `
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                    <path d="M12 17C14.7614 17 17 14.7614 17 12C17 9.23858 14.7614 7 12 7C9.23858 7 7 9.23858 7 12C7 14.7614 9.23858 17 12 17Z" fill="currentColor"/>
                    <path d="M12 1V3M12 21V23M4.22 4.22L5.64 5.64M18.36 18.36L19.78 19.78M1 12H3M21 12H23M4.22 19.78L5.64 18.36M18.36 5.64L19.78 4.22" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                </svg>
            `;
        } else {
            themeToggle.innerHTML = `
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                    <path d="M12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22ZM12 20V4C16.4183 4 20 7.58172 20 12C20 16.4183 16.4183 20 12 20Z" fill="currentColor"/>
                </svg>
            `;
        }
    }

    handleKeyboardShortcuts(e) {
        // Ctrl/Cmd + K to focus chat input
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            const chatInput = document.getElementById('chatInput');
            if (chatInput && !chatInput.disabled) {
                chatInput.focus();
            }
        }

        // Escape to close modals/panels
        if (e.key === 'Escape') {
            // Close error modal
            const errorModal = document.getElementById('errorModal');
            if (errorModal && !errorModal.classList.contains('hidden')) {
                ErrorHandler.hideError();
                return;
            }

            // Close history panel
            const historyPanel = document.getElementById('historyPanel');
            if (historyPanel && historyPanel.classList.contains('active')) {
                chatBot.toggleHistoryPanel();
                return;
            }

            // Close user dropdown
            const userDropdown = document.getElementById('userDropdown');
            if (userDropdown && userDropdown.classList.contains('active')) {
                userDropdown.classList.remove('active');
                return;
            }
        }

        // Ctrl/Cmd + N for new chat
        if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
            e.preventDefault();
            if (authManager.isUserAuthenticated()) {
                chatBot.startNewConversation();
            }
        }

        // Ctrl/Cmd + H for history
        if ((e.ctrlKey || e.metaKey) && e.key === 'h') {
            e.preventDefault();
            if (authManager.isUserAuthenticated()) {
                chatBot.toggleHistoryPanel();
            }
        }
    }

    handleOutsideClick(e) {
        // Close user dropdown if clicking outside
        const userMenu = document.getElementById('userMenu');
        const userDropdown = document.getElementById('userDropdown');
        
        if (userDropdown && userMenu && 
            userDropdown.classList.contains('active') &&
            !userMenu.contains(e.target)) {
            userDropdown.classList.remove('active');
        }
    }

    onUserAuthenticated(user) {
        this.currentScreen = 'chat';
        this.updateConnectionStatus();
        
        // Show welcome notification
        this.showNotification(`환영합니다, ${user.name || '사용자'}님! 🎉`, 'success');
    }

    onUserUnauthenticated() {
        this.currentScreen = 'login';
    }

    onUserLoggedOut() {
        this.currentScreen = 'login';
        this.showNotification('안전하게 로그아웃되었습니다. 👋', 'info');
    }

    onSessionExpired() {
        this.showNotification('세션이 만료되었습니다. 다시 로그인해주세요. ⏰', 'warning');
    }

    onThemeChanged(theme) {
        const message = theme === CONFIG.THEME.DARK 
            ? '다크 모드가 활성화되었습니다. 🌙' 
            : '라이트 모드가 활성화되었습니다. ☀️';
        
        this.showNotification(message, 'info');
    }

    onError(errorData) {
        // Additional error handling logic can be added here
        console.group('🔍 에러 상세 정보');
        console.error('Context:', errorData.context);
        console.error('Error:', errorData.error);
        console.error('Message:', errorData.message);
        console.groupEnd();
    }

    onConnectionStatusChange(isOnline) {
        if (isOnline) {
            this.showNotification('인터넷 연결이 복구되었습니다. 🌐', 'success');
            
            // Retry any failed operations if needed
            if (authManager.isUserAuthenticated()) {
                // Optionally recheck session
                authManager.checkSession();
            }
        } else {
            this.showNotification('인터넷 연결이 끊어졌습니다. 연결을 확인해주세요. 📡', 'warning');
        }
    }

    onVisibilityChange() {
        if (!document.hidden && authManager.isUserAuthenticated()) {
            // Page became visible, check session validity
            authManager.checkSession();
        }
    }

    updateConnectionStatus() {
        // Update UI based on connection status
        const isOnline = navigator.onLine;
        const statusElement = document.getElementById('connectionStatus');
        
        if (statusElement) {
            statusElement.textContent = isOnline ? '온라인' : '오프라인';
            statusElement.className = isOnline ? 'status-online' : 'status-offline';
        }
    }

    showNotification(message, type = 'info', duration = 3000) {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <span class="notification-message">${Utils.escapeHtml(message)}</span>
                <button class="notification-close">&times;</button>
            </div>
        `;

        // Add to document
        document.body.appendChild(notification);

        // Show notification
        setTimeout(() => {
            notification.classList.add('show');
        }, 10);

        // Auto hide
        const hideTimeout = setTimeout(() => {
            this.hideNotification(notification);
        }, duration);

        // Manual close
        const closeBtn = notification.querySelector('.notification-close');
        closeBtn.addEventListener('click', () => {
            clearTimeout(hideTimeout);
            this.hideNotification(notification);
        });
    }

    hideNotification(notification) {
        notification.classList.remove('show');
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }

    // Public API methods
    getCurrentScreen() {
        return this.currentScreen;
    }

    isAppInitialized() {
        return this.isInitialized;
    }

    getAppInfo() {
        return {
            version: '1.0.0',
            buildDate: new Date().toISOString(),
            isInitialized: this.isInitialized,
            currentScreen: this.currentScreen,
            theme: ThemeManager.getTheme(),
            isAuthenticated: authManager.isUserAuthenticated(),
            user: authManager.getCurrentUser(),
            storageInfo: Storage.getStorageInfo()
        };
    }

    // Debug methods
    debug() {
        console.group('🔧 애플리케이션 디버그 정보');
        console.log('App Info:', this.getAppInfo());
        console.log('Auth Manager:', authManager);
        console.log('Chat Bot:', chatBot);
        console.log('Event Bus:', eventBus);
        console.log('Storage:', Storage.getStorageInfo());
        console.groupEnd();
    }

    // Export application data
    async exportData() {
        try {
            const data = {
                appInfo: this.getAppInfo(),
                conversations: chatBot.getConversations(),
                theme: ThemeManager.getTheme(),
                exportDate: new Date().toISOString()
            };

            const blob = new Blob([JSON.stringify(data, null, 2)], { 
                type: 'application/json' 
            });
            
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `aiminder_export_${Utils.formatDate(new Date(), {
                year: 'numeric',
                month: '2-digit', 
                day: '2-digit'
            }).replace(/\//g, '-')}.json`;
            a.click();
            
            URL.revokeObjectURL(url);
            
            this.showNotification('데이터를 성공적으로 내보냈습니다. 📥', 'success');
        } catch (error) {
            ErrorHandler.handle(error, 'App.exportData');
        }
    }

    // Reset application
    async resetApp() {
        if (!confirm('모든 데이터가 삭제됩니다. 계속하시겠습니까?')) {
            return;
        }

        try {
            // Logout user
            if (authManager.isUserAuthenticated()) {
                await authManager.logout();
            }

            // Clear all storage
            Storage.clear();

            // Reset theme
            ThemeManager.setTheme(CONFIG.THEME.LIGHT);

            // Reload page
            window.location.reload();
        } catch (error) {
            ErrorHandler.handle(error, 'App.resetApp');
        }
    }

    cleanup() {
        console.log('🧹 애플리케이션 정리 중...');
        
        // Cleanup auth manager
        if (authManager && typeof authManager.destroy === 'function') {
            authManager.destroy();
        }

        // Clear any intervals or timeouts
        // Additional cleanup logic can be added here
    }
}

// Additional CSS for notifications (inject into head)
const notificationStyles = `
<style>
.notification {
    position: fixed;
    top: 20px;
    right: 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
    z-index: 10000;
    transform: translateX(100%);
    transition: transform 0.3s ease;
    max-width: 400px;
    min-width: 300px;
}

.notification.show {
    transform: translateX(0);
}

.notification-content {
    padding: 16px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.notification-message {
    flex: 1;
    font-size: 14px;
    line-height: 1.4;
}

.notification-close {
    background: none;
    border: none;
    font-size: 18px;
    cursor: pointer;
    color: #666;
    padding: 4px;
    border-radius: 4px;
    transition: background-color 0.2s ease;
}

.notification-close:hover {
    background: rgba(0, 0, 0, 0.1);
}

.notification-success {
    border-left: 4px solid #34c759;
}

.notification-error {
    border-left: 4px solid #ff3b30;
}

.notification-warning {
    border-left: 4px solid #ff9500;
}

.notification-info {
    border-left: 4px solid #007aff;
}

body.dark-theme .notification {
    background: #333;
    color: #f5f5f5;
}

body.dark-theme .notification-close {
    color: #bbb;
}
</style>
`;

// Inject notification styles
document.head.insertAdjacentHTML('beforeend', notificationStyles);

// Initialize application
const app = new App();

// Export for global access and debugging
window.app = app;

// Development helpers
if (process?.env?.NODE_ENV === 'development' || window.location.hostname === 'localhost') {
    window.debug = () => app.debug();
    window.exportData = () => app.exportData();
    window.resetApp = () => app.resetApp();
    
    console.log('🛠️ 개발 모드 헬퍼 함수가 활성화되었습니다:');
    console.log('- debug(): 디버그 정보 출력');
    console.log('- exportData(): 데이터 내보내기');
    console.log('- resetApp(): 앱 초기화');
}