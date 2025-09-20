/**
 * Utility functions for the integrated application
 */

// Configuration
const CONFIG = {
    API_BASE_URL: 'http://localhost:8080',
    STORAGE_KEYS: {
        CONVERSATIONS: 'aiminder_conversations',
        THEME: 'aiminder_theme',
        USER_PREFS: 'aiminder_user_prefs'
    },
    THEME: {
        LIGHT: 'light',
        DARK: 'dark'
    },
    ANIMATION_DURATION: {
        FAST: 150,
        NORMAL: 300,
        SLOW: 500
    }
};

// Event system for inter-module communication
class EventBus {
    constructor() {
        this.events = {};
    }

    on(event, callback) {
        if (!this.events[event]) {
            this.events[event] = [];
        }
        this.events[event].push(callback);
    }

    off(event, callback) {
        if (!this.events[event]) return;
        this.events[event] = this.events[event].filter(cb => cb !== callback);
    }

    emit(event, data) {
        if (!this.events[event]) return;
        this.events[event].forEach(callback => {
            try {
                callback(data);
            } catch (error) {
                console.error('Event callback error:', error);
            }
        });
    }
}

// Global event bus instance
const eventBus = new EventBus();

// Utility Functions
const Utils = {
    /**
     * Generate UUID v4
     */
    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    },

    /**
     * Format date to locale string
     */
    formatDate(date, options = {}) {
        const defaultOptions = {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        };
        return new Date(date).toLocaleString('ko-KR', { ...defaultOptions, ...options });
    },

    /**
     * Format time to locale string
     */
    formatTime(date) {
        return new Date(date).toLocaleTimeString('ko-KR', {
            hour: '2-digit',
            minute: '2-digit'
        });
    },

    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },

    /**
     * Debounce function execution
     */
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func.apply(this, args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    /**
     * Throttle function execution
     */
    throttle(func, limit) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },

    /**
     * Deep clone object
     */
    deepClone(obj) {
        if (obj === null || typeof obj !== 'object') return obj;
        if (obj instanceof Date) return new Date(obj.getTime());
        if (obj instanceof Array) return obj.map(item => this.deepClone(item));
        if (typeof obj === 'object') {
            const copy = {};
            Object.keys(obj).forEach(key => {
                copy[key] = this.deepClone(obj[key]);
            });
            return copy;
        }
    },

    /**
     * Check if user is on mobile device
     */
    isMobile() {
        return window.innerWidth <= 768 || /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    },

    /**
     * Smooth scroll to element
     */
    scrollToElement(element, options = {}) {
        if (!element) return;
        
        const defaultOptions = {
            behavior: 'smooth',
            block: 'nearest',
            inline: 'nearest'
        };
        
        element.scrollIntoView({ ...defaultOptions, ...options });
    },

    /**
     * Add CSS class with transition
     */
    addClassWithTransition(element, className, duration = CONFIG.ANIMATION_DURATION.NORMAL) {
        element.classList.add(className);
        return new Promise(resolve => {
            setTimeout(resolve, duration);
        });
    },

    /**
     * Remove CSS class with transition
     */
    removeClassWithTransition(element, className, duration = CONFIG.ANIMATION_DURATION.NORMAL) {
        element.classList.remove(className);
        return new Promise(resolve => {
            setTimeout(resolve, duration);
        });
    },

    /**
     * Show element with fade in animation
     */
    async showElement(element, duration = CONFIG.ANIMATION_DURATION.NORMAL) {
        element.style.opacity = '0';
        element.classList.remove('hidden');
        element.style.transition = `opacity ${duration}ms ease`;
        
        // Force reflow
        element.offsetHeight;
        
        element.style.opacity = '1';
        
        return new Promise(resolve => {
            setTimeout(() => {
                element.style.transition = '';
                resolve();
            }, duration);
        });
    },

    /**
     * Hide element with fade out animation
     */
    async hideElement(element, duration = CONFIG.ANIMATION_DURATION.NORMAL) {
        element.style.transition = `opacity ${duration}ms ease`;
        element.style.opacity = '0';
        
        return new Promise(resolve => {
            setTimeout(() => {
                element.classList.add('hidden');
                element.style.transition = '';
                element.style.opacity = '';
                resolve();
            }, duration);
        });
    },

    /**
     * Copy text to clipboard
     */
    async copyToClipboard(text) {
        try {
            if (navigator.clipboard) {
                await navigator.clipboard.writeText(text);
            } else {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = text;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
            }
            return true;
        } catch (error) {
            console.error('Failed to copy text:', error);
            return false;
        }
    },

    /**
     * Format message text with markdown-like syntax
     */
    formatMessage(text) {
        if (!text) return '';
        
        let formattedText = this.escapeHtml(text);

        // Line breaks
        formattedText = formattedText.replace(/\n/g, '<br>');

        // Bold text **text**
        formattedText = formattedText.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

        // Italic text *text*
        formattedText = formattedText.replace(/\*(.*?)\*/g, '<em>$1</em>');

        // Quoted text "text"
        formattedText = formattedText.replace(/"([^"]+)"/g, '<span class="highlight">$1</span>');

        // Bullet points
        formattedText = formattedText.replace(/^• (.+)$/gm, '<li>$1</li>');
        if (formattedText.includes('<li>')) {
            formattedText = formattedText.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
        }

        // Numbered bullets (①②③...)
        formattedText = formattedText.replace(/[①②③④⑤⑥⑦⑧⑨⑩]/g, (match) => {
            return `<strong style="color: var(--primary-color);">${match}</strong>`;
        });

        return formattedText;
    },

    /**
     * Validate email format
     */
    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },

    /**
     * Get user's preferred language
     */
    getUserLanguage() {
        return navigator.language || navigator.userLanguage || 'ko-KR';
    },

    /**
     * Format file size
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    /**
     * Get relative time string
     */
    getRelativeTime(date) {
        const now = new Date();
        const diffMs = now - new Date(date);
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return '방금 전';
        if (diffMins < 60) return `${diffMins}분 전`;
        if (diffHours < 24) return `${diffHours}시간 전`;
        if (diffDays < 7) return `${diffDays}일 전`;
        
        return this.formatDate(date, { month: 'short', day: 'numeric' });
    }
};

// Local Storage Management
const Storage = {
    /**
     * Set item in localStorage with error handling
     */
    setItem(key, value) {
        try {
            localStorage.setItem(key, JSON.stringify(value));
            return true;
        } catch (error) {
            console.error('Failed to save to localStorage:', error);
            return false;
        }
    },

    /**
     * Get item from localStorage with error handling
     */
    getItem(key, defaultValue = null) {
        try {
            const item = localStorage.getItem(key);
            return item ? JSON.parse(item) : defaultValue;
        } catch (error) {
            console.error('Failed to read from localStorage:', error);
            return defaultValue;
        }
    },

    /**
     * Remove item from localStorage
     */
    removeItem(key) {
        try {
            localStorage.removeItem(key);
            return true;
        } catch (error) {
            console.error('Failed to remove from localStorage:', error);
            return false;
        }
    },

    /**
     * Clear all localStorage data
     */
    clear() {
        try {
            localStorage.clear();
            return true;
        } catch (error) {
            console.error('Failed to clear localStorage:', error);
            return false;
        }
    },

    /**
     * Get storage usage information
     */
    getStorageInfo() {
        try {
            let totalSize = 0;
            for (let key in localStorage) {
                if (localStorage.hasOwnProperty(key)) {
                    totalSize += localStorage[key].length + key.length;
                }
            }
            return {
                used: totalSize,
                usedFormatted: Utils.formatFileSize(totalSize),
                available: 5242880 - totalSize, // 5MB limit
                availableFormatted: Utils.formatFileSize(5242880 - totalSize)
            };
        } catch (error) {
            console.error('Failed to get storage info:', error);
            return null;
        }
    }
};

// Theme Management
const ThemeManager = {
    /**
     * Get current theme
     */
    getTheme() {
        return Storage.getItem(CONFIG.STORAGE_KEYS.THEME, CONFIG.THEME.LIGHT);
    },

    /**
     * Set theme
     */
    setTheme(theme) {
        if (!Object.values(CONFIG.THEME).includes(theme)) {
            console.warn('Invalid theme:', theme);
            return false;
        }

        Storage.setItem(CONFIG.STORAGE_KEYS.THEME, theme);
        this.applyTheme(theme);
        eventBus.emit('themeChanged', theme);
        return true;
    },

    /**
     * Toggle between light and dark theme
     */
    toggleTheme() {
        const currentTheme = this.getTheme();
        const newTheme = currentTheme === CONFIG.THEME.LIGHT 
            ? CONFIG.THEME.DARK 
            : CONFIG.THEME.LIGHT;
        return this.setTheme(newTheme);
    },

    /**
     * Apply theme to document
     */
    applyTheme(theme) {
        if (theme === CONFIG.THEME.DARK) {
            document.body.classList.add('dark-theme');
        } else {
            document.body.classList.remove('dark-theme');
        }
    },

    /**
     * Initialize theme from storage
     */
    init() {
        const savedTheme = this.getTheme();
        this.applyTheme(savedTheme);
    }
};

// Error Handler
const ErrorHandler = {
    /**
     * Handle and display errors
     */
    handle(error, context = '') {
        console.error(`Error in ${context}:`, error);
        
        let message = '알 수 없는 오류가 발생했습니다.';
        
        if (error.message) {
            message = error.message;
        } else if (typeof error === 'string') {
            message = error;
        }

        this.showError(message);
        
        // Emit error event for other modules to handle
        eventBus.emit('error', { error, context, message });
    },

    /**
     * Show error modal or notification
     */
    showError(message) {
        const errorModal = document.getElementById('errorModal');
        const errorMessage = document.getElementById('errorMessage');
        
        if (errorModal && errorMessage) {
            errorMessage.textContent = message;
            errorModal.classList.add('active');
        } else {
            // Fallback to alert if modal is not available
            alert(message);
        }
    },

    /**
     * Hide error modal
     */
    hideError() {
        const errorModal = document.getElementById('errorModal');
        if (errorModal) {
            errorModal.classList.remove('active');
        }
    }
};

// Export for global access
window.CONFIG = CONFIG;
window.eventBus = eventBus;
window.Utils = Utils;
window.Storage = Storage;
window.ThemeManager = ThemeManager;
window.ErrorHandler = ErrorHandler;