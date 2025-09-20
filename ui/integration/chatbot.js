/**
 * Chatbot module for AI conversation management
 */

class ChatBot {
    constructor() {
        this.conversationId = null;
        this.conversations = [];
        this.currentUser = null;
        this.isInitialized = false;
        
        // DOM elements
        this.chatMessages = null;
        this.messageContainer = null;
        this.chatForm = null;
        this.chatInput = null;
        this.sendBtn = null;
        this.typingIndicator = null;
        this.historyBtn = null;
        this.historyPanel = null;
        this.closeHistory = null;
        this.conversationList = null;
        this.newChatBtn = null;
        
        this.init();
    }

    init() {
        this.bindDOMElements();
        this.bindEvents();
        this.setupEventListeners();
    }

    bindDOMElements() {
        this.chatMessages = document.getElementById('chatMessages');
        this.messageContainer = document.getElementById('messageContainer');
        this.chatForm = document.getElementById('chatForm');
        this.chatInput = document.getElementById('chatInput');
        this.sendBtn = document.getElementById('sendBtn');
        this.typingIndicator = document.getElementById('typingIndicator');
        this.historyBtn = document.getElementById('historyBtn');
        this.historyPanel = document.getElementById('historyPanel');
        this.closeHistory = document.getElementById('closeHistory');
        this.conversationList = document.getElementById('conversationList');
        this.newChatBtn = document.getElementById('newChatBtn');
    }

    bindEvents() {
        if (this.chatForm) {
            this.chatForm.addEventListener('submit', (e) => this.handleSubmit(e));
        }

        if (this.chatInput) {
            this.chatInput.addEventListener('keydown', (e) => this.handleInputKeydown(e));
            this.chatInput.addEventListener('input', () => this.autoResizeTextarea());
        }

        if (this.historyBtn) {
            this.historyBtn.addEventListener('click', () => this.toggleHistoryPanel());
        }

        if (this.closeHistory) {
            this.closeHistory.addEventListener('click', () => this.toggleHistoryPanel());
        }

        if (this.newChatBtn) {
            this.newChatBtn.addEventListener('click', () => {
                this.startNewConversation();
                this.toggleHistoryPanel();
            });
        }
    }

    setupEventListeners() {
        // Listen for authentication events
        eventBus.on('userAuthenticated', (user) => {
            this.handleUserAuthenticated(user);
        });

        eventBus.on('userUnauthenticated', () => {
            this.handleUserUnauthenticated();
        });

        eventBus.on('userLoggedOut', () => {
            this.handleUserLoggedOut();
        });

        eventBus.on('sessionExpired', () => {
            this.handleSessionExpired();
        });
    }

    /**
     * Handle user authentication
     */
    async handleUserAuthenticated(user) {
        this.currentUser = user;
        await this.loadUserConversations();
        
        if (!this.conversationId || this.conversations.length === 0) {
            this.startNewConversation();
        } else {
            this.loadConversationHistory();
        }
        
        this.isInitialized = true;
        console.log('ChatBot initialized for user:', user);
    }

    /**
     * Handle user unauthentication
     */
    handleUserUnauthenticated() {
        this.currentUser = null;
        this.isInitialized = false;
        this.conversations = [];
        this.conversationId = null;
        
        if (this.messageContainer) {
            this.messageContainer.innerHTML = '';
        }
    }

    /**
     * Handle user logout
     */
    handleUserLoggedOut() {
        this.handleUserUnauthenticated();
    }

    /**
     * Handle session expiration
     */
    handleSessionExpired() {
        this.displayError('세션이 만료되었습니다. 다시 로그인해주세요.');
        this.handleUserUnauthenticated();
    }

    /**
     * Load user-specific conversations
     */
    async loadUserConversations() {
        if (!this.currentUser) return;
        
        const userStorageKey = this.getUserStorageKey(CONFIG.STORAGE_KEYS.CONVERSATIONS);
        this.conversations = Storage.getItem(userStorageKey, []);
    }

    /**
     * Save user-specific conversations
     */
    saveConversations() {
        if (!this.currentUser) return;
        
        const userStorageKey = this.getUserStorageKey(CONFIG.STORAGE_KEYS.CONVERSATIONS);
        Storage.setItem(userStorageKey, this.conversations);
    }

    /**
     * Get user-specific storage key
     */
    getUserStorageKey(key) {
        if (!this.currentUser) return key;
        return `${this.currentUser.id}_${key}`;
    }

    /**
     * Start a new conversation
     */
    startNewConversation() {
        if (!this.currentUser) {
            console.warn('Cannot start conversation without authenticated user');
            return;
        }

        // Generate new conversation ID
        this.conversationId = Utils.generateUUID();

        // Clear message container
        if (this.messageContainer) {
            this.messageContainer.innerHTML = '';
        }

        // Create new conversation
        const newConversation = {
            id: this.conversationId,
            title: '새 대화',
            date: new Date().toISOString(),
            messages: [],
            userId: this.currentUser.id
        };

        this.conversations.push(newConversation);
        this.saveConversations();
        this.loadConversationHistory();
        this.showWelcomeMessage();

        console.log('Started new conversation:', this.conversationId);
    }

    /**
     * Load conversation by index
     */
    loadConversation(index) {
        const conversation = this.conversations[index];
        if (!conversation) return;

        this.conversationId = conversation.id;
        
        if (this.messageContainer) {
            this.messageContainer.innerHTML = '';
        }

        // Display saved messages
        conversation.messages.forEach(msg => {
            if (msg.sender === 'user') {
                this.displayUserMessage(msg.text, msg.time, false);
            } else if (msg.sender === 'assistant') {
                this.displayAssistantMessage(msg.response, msg.time, false);
            }
        });

        this.scrollToBottom();
    }

    /**
     * Update conversation history display
     */
    loadConversationHistory() {
        if (!this.conversationList) return;
        
        this.conversationList.innerHTML = '';

        if (this.conversations.length === 0) {
            const emptyMessage = document.createElement('div');
            emptyMessage.className = 'conversation-item';
            emptyMessage.innerHTML = '<div class="conversation-title">아직 대화 기록이 없습니다.</div>';
            this.conversationList.appendChild(emptyMessage);
            return;
        }

        // Display conversations in reverse order (most recent first)
        this.conversations.slice().reverse().forEach((conversation, index) => {
            const item = document.createElement('div');
            item.className = 'conversation-item';
            
            // Highlight current conversation
            if (conversation.id === this.conversationId) {
                item.classList.add('active');
            }
            
            item.innerHTML = `
                <div class="conversation-title">${Utils.escapeHtml(conversation.title || '제목 없는 대화')}</div>
                <div class="conversation-date">${Utils.formatDate(conversation.date)}</div>
            `;

            item.addEventListener('click', () => {
                this.loadConversation(this.conversations.length - 1 - index);
                this.toggleHistoryPanel();
            });

            this.conversationList.appendChild(item);
        });
    }

    /**
     * Toggle history panel
     */
    toggleHistoryPanel() {
        if (this.historyPanel) {
            this.historyPanel.classList.toggle('active');
        }
    }

    /**
     * Show welcome message
     */
    showWelcomeMessage() {
        const welcomeResponse = {
            responses: [
                {
                    type: "TEXT",
                    messages: [`안녕하세요, ${this.currentUser?.name || '사용자'}님! 👋 저는 당신의 AI 목표 코칭 비서입니다. 지금부터 목표 달성 여정을 함께 설계해 봐요. 먼저, 목표를 명확히 파악해야 해요. 🎯 이루고자 하는 목표는 무엇인가요❓`]
                },
                {
                    type: "QUICK_REPLIES",
                    messages: ["다이어트 💪", "경제적 자유 💰", "자격증 취득 🏅", "새로운 습관 만들기 🌱"]
                }
            ]
        };

        const time = this.saveMessage('assistant', null, welcomeResponse);
        this.displayAssistantMessage(welcomeResponse, time, false);
    }

    /**
     * Handle form submission
     */
    async handleSubmit(e) {
        e.preventDefault();
        
        if (!this.isInitialized) {
            this.displayError('채팅을 시작하려면 로그인이 필요합니다.');
            return;
        }

        if (!this.chatInput) return;
        
        const message = this.chatInput.value.trim();
        if (!message) return;

        // Display user message
        const time = this.saveMessage('user', message);
        this.displayUserMessage(message, time);

        // Update conversation title if this is the first message
        this.updateConversationTitle(message);

        // Clear input
        this.chatInput.value = '';
        this.chatInput.style.height = 'auto';
        
        // Set loading state
        this.setLoading(true);

        try {
            // Send message to API
            const response = await this.sendMessage(message);
            const responseTime = this.saveMessage('assistant', null, response);
            this.displayAssistantMessage(response, responseTime);
        } catch (error) {
            this.displayError('죄송합니다. 서버와 연결할 수 없습니다. 잠시 후 다시 시도해주세요. 🔄');
            console.error('API 호출 오류:', error);
        } finally {
            this.setLoading(false);
        }
    }

    /**
     * Handle input keydown events
     */
    handleInputKeydown(e) {
        // Shift+Enter for line break, Enter for send
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            this.handleSubmit(e);
        }

        // Tab for indentation
        if (e.key === 'Tab') {
            e.preventDefault();
            const start = this.chatInput.selectionStart;
            const end = this.chatInput.selectionEnd;
            this.chatInput.value = this.chatInput.value.substring(0, start) + '  ' + this.chatInput.value.substring(end);
            this.chatInput.selectionStart = this.chatInput.selectionEnd = start + 2;
        }
    }

    /**
     * Auto-resize textarea
     */
    autoResizeTextarea() {
        if (!this.chatInput) return;
        
        this.chatInput.style.height = 'auto';
        this.chatInput.style.height = this.chatInput.scrollHeight + 'px';

        // Limit max height
        if (this.chatInput.scrollHeight > 120) {
            this.chatInput.style.overflowY = 'auto';
        } else {
            this.chatInput.style.overflowY = 'hidden';
        }
    }

    /**
     * Send message to AI API
     */
    async sendMessage(text) {
        if (!authManager.isUserAuthenticated()) {
            throw new Error('인증이 필요합니다.');
        }

        const response = await authManager.makeAuthenticatedRequest(
            `${CONFIG.API_BASE_URL}/chat/${this.conversationId}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ text })
            }
        );

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    }

    /**
     * Display user message
     */
    displayUserMessage(message, time = null, shouldSave = false) {
        if (!this.messageContainer) return;

        const messageDiv = document.createElement('div');
        messageDiv.className = 'message user';

        const formattedTime = time 
            ? Utils.formatTime(time) 
            : Utils.formatTime(new Date());

        messageDiv.innerHTML = `
            <div class="message-bubble">${Utils.escapeHtml(message)}</div>
            <div class="message-time">${formattedTime}</div>
        `;

        this.messageContainer.appendChild(messageDiv);
        this.scrollToBottom();

        if (shouldSave) {
            this.saveMessage('user', message);
        }
    }

    /**
     * Display assistant message
     */
    displayAssistantMessage(response, time = null, shouldSave = false) {
        if (!this.messageContainer || !response.responses || !Array.isArray(response.responses)) {
            this.displayError('서버에서 잘못된 응답을 받았습니다. 🚫');
            return;
        }

        const formattedTime = time 
            ? Utils.formatTime(time) 
            : Utils.formatTime(new Date());

        response.responses.forEach(item => {
            const messageDiv = document.createElement('div');
            messageDiv.className = 'message assistant';

            if (item.type === 'TEXT') {
                const textContent = Array.isArray(item.messages) ? item.messages[0] : item.messages;
                messageDiv.innerHTML = `
                    <div class="message-bubble">${Utils.formatMessage(textContent)}</div>
                    <div class="message-time">${formattedTime}</div>
                `;
            } else if (item.type === 'QUICK_REPLIES') {
                const quickRepliesHtml = item.messages.map(reply =>
                    `<button class="quick-reply-btn" data-reply="${Utils.escapeHtml(reply)}">${Utils.escapeHtml(reply)}</button>`
                ).join('');

                messageDiv.innerHTML = `
                    <div class="quick-replies">${quickRepliesHtml}</div>
                `;

                // Bind quick reply events
                messageDiv.querySelectorAll('.quick-reply-btn').forEach(btn => {
                    btn.addEventListener('click', () => {
                        this.handleQuickReply(btn.dataset.reply);
                    });
                });
            }

            this.messageContainer.appendChild(messageDiv);
        });

        this.scrollToBottom();

        if (shouldSave) {
            this.saveMessage('assistant', null, response);
        }
    }

    /**
     * Handle quick reply button click
     */
    async handleQuickReply(reply) {
        // Disable all quick reply buttons
        const quickReplyBtns = document.querySelectorAll('.quick-reply-btn');
        quickReplyBtns.forEach(btn => {
            btn.disabled = true;
            btn.style.opacity = '0.6';
        });

        // Display as user message and send to server
        const time = this.saveMessage('user', reply);
        this.displayUserMessage(reply, time);

        // Update conversation title if this is the first message
        this.updateConversationTitle(reply);

        this.setLoading(true);

        try {
            const response = await this.sendMessage(reply);
            const responseTime = this.saveMessage('assistant', null, response);
            this.displayAssistantMessage(response, responseTime);
        } catch (error) {
            this.displayError('죄송합니다. 서버와 연결할 수 없습니다. 잠시 후 다시 시도해주세요. 🔄');
            console.error('API 호출 오류:', error);
        } finally {
            this.setLoading(false);
        }
    }

    /**
     * Display error message
     */
    displayError(message) {
        if (!this.messageContainer) return;

        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.innerHTML = `⚠️ ${Utils.escapeHtml(message)}`;
        this.messageContainer.appendChild(errorDiv);
        this.scrollToBottom();
    }

    /**
     * Set loading state
     */
    setLoading(isLoading) {
        if (this.sendBtn) {
            this.sendBtn.disabled = isLoading;
        }
        
        if (this.chatInput) {
            this.chatInput.disabled = isLoading;
        }
        
        if (this.typingIndicator) {
            this.typingIndicator.style.display = isLoading ? 'flex' : 'none';
        }

        if (isLoading && this.typingIndicator) {
            // Scroll to show typing indicator
            setTimeout(() => {
                this.typingIndicator.scrollIntoView({ 
                    behavior: 'smooth', 
                    block: 'end' 
                });
            }, 100);
        }
    }

    /**
     * Save message to conversation
     */
    saveMessage(sender, text, response = null) {
        if (!this.currentUser || !this.conversationId) return null;

        const currentConversation = this.conversations.find(c => c.id === this.conversationId);
        if (!currentConversation) return null;

        const time = new Date().toISOString();
        const message = {
            sender,
            text,
            time
        };

        if (response) {
            message.response = response;
        }

        currentConversation.messages.push(message);
        this.saveConversations();

        return time;
    }

    /**
     * Update conversation title with first message
     */
    updateConversationTitle(text) {
        if (!this.conversationId) return;

        const currentConversation = this.conversations.find(c => c.id === this.conversationId);
        if (currentConversation && currentConversation.title === '새 대화') {
            // Limit title to 30 characters
            currentConversation.title = text.length > 30 
                ? text.substring(0, 27) + '...' 
                : text;
            this.saveConversations();
            this.loadConversationHistory();
        }
    }

    /**
     * Scroll to bottom of chat
     */
    scrollToBottom() {
        if (this.chatMessages) {
            setTimeout(() => {
                this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
            }, 100);
        }
    }

    /**
     * Get current conversation ID
     */
    getCurrentConversationId() {
        return this.conversationId;
    }

    /**
     * Get all conversations for current user
     */
    getConversations() {
        return this.conversations;
    }

    /**
     * Delete conversation
     */
    deleteConversation(conversationId) {
        this.conversations = this.conversations.filter(c => c.id !== conversationId);
        this.saveConversations();
        this.loadConversationHistory();

        // If deleted conversation was current, start new one
        if (this.conversationId === conversationId) {
            this.startNewConversation();
        }
    }

    /**
     * Clear all conversations for current user
     */
    clearAllConversations() {
        this.conversations = [];
        this.saveConversations();
        this.loadConversationHistory();
        this.startNewConversation();
    }

    /**
     * Export conversations
     */
    exportConversations() {
        const data = {
            user: this.currentUser,
            conversations: this.conversations,
            exportDate: new Date().toISOString()
        };

        const blob = new Blob([JSON.stringify(data, null, 2)], { 
            type: 'application/json' 
        });
        
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `aiminder_conversations_${Utils.formatDate(new Date(), { 
            year: 'numeric', 
            month: '2-digit', 
            day: '2-digit' 
        }).replace(/\//g, '-')}.json`;
        a.click();
        
        URL.revokeObjectURL(url);
    }
}

// Initialize chatbot
const chatBot = new ChatBot();

// Export for global access
window.chatBot = chatBot;