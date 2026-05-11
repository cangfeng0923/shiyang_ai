// chat.js - 最终修复版（正确滚动 + 正确时间显示）

let chatHistory = [];
let currentStreamingIndex = null;
let accumulatedReply = '';
let isLoadingHistory = false; // 防止重复加载

// 等待用户加载完成的辅助函数
async function waitForUser(maxWaitTime = 3000) {
    if (window.currentUser) return true;

    const savedUser = localStorage.getItem('currentUser');
    if (savedUser) {
        try {
            window.currentUser = JSON.parse(savedUser);
            if (window.currentUser) return true;
        } catch(e) {}
    }

    return new Promise((resolve) => {
        const startTime = Date.now();
        const checkInterval = setInterval(() => {
            if (window.currentUser) {
                clearInterval(checkInterval);
                resolve(true);
            } else if (Date.now() - startTime > maxWaitTime) {
                clearInterval(checkInterval);
                resolve(false);
            }
        }, 100);
    });
}

// 初始化聊天功能
async function initChat() {
    // 防止重复初始化
    if (window._chatInitialized) {
        console.log('聊天已初始化，跳过');
        return;
    }

    const hasUser = await waitForUser();

    if (!hasUser || !window.currentUser) {
        console.error('用户未登录，无法初始化聊天');
        const container = document.getElementById('messagesContainer');
        if (container && container.children.length === 0) {
            container.innerHTML = `
                <div class="message ai-message">
                    <div class="message-avatar">🤖</div>
                    <div class="message-bubble">
                        <div class="message-content">
                            ⚠️ 请先登录后再使用聊天功能<br>
                            <small>请返回首页重新登录</small>
                        </div>
                        <div class="message-time">${getCurrentTime()}</div>
                    </div>
                </div>
            `;
        }
        return;
    }

    console.log('聊天模块初始化成功，用户:', window.currentUser.username);
    window._chatInitialized = true;
    await loadChatHistory();

    const messageInput = document.getElementById('messageInput');
    if (messageInput) {
        messageInput.removeEventListener('keypress', handleKeyPress);
        messageInput.addEventListener('keypress', handleKeyPress);
    }
}

function handleKeyPress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// 发送消息
async function sendMessage() {
    console.log('sendMessage 被调用');

    if (!window.currentUser) {
        const savedUser = localStorage.getItem('currentUser');
        if (!savedUser) {
            alert('请先登录后再发送消息');
            return;
        }
        try {
            window.currentUser = JSON.parse(savedUser);
        } catch(e) {
            alert('请先登录后再发送消息');
            return;
        }
    }

    const inputElement = document.getElementById('messageInput');
    if (!inputElement) {
        console.error('messageInput 元素不存在');
        return;
    }

    const message = inputElement.value.trim();

    if (!message) {
        alert('请输入消息内容');
        return;
    }

    let constitution = '平和质';
    if (window.currentUser && window.currentUser.constitution) {
        constitution = window.currentUser.constitution;
    }

    toggleInputState(false);

    const now = new Date();
    addMessageToUI(message, true, now.toISOString());
    inputElement.value = '';

    const aiMessageId = addAIPlaceholder();
    const aiStartTime = new Date();

    try {
        await sendMessageWithSSE(message, constitution, aiMessageId, aiStartTime);
    } catch (error) {
        console.error('SSE失败，使用HTTP接口:', error);
        await sendMessageWithHTTP(message, constitution, aiMessageId, aiStartTime);
    } finally {
        toggleInputState(true);
    }
}

// 使用SSE流式接口
async function sendMessageWithSSE(message, constitution, messageId, startTime) {
    return new Promise((resolve, reject) => {
        const url = `${API_BASE}/api/chat/stream?userId=${window.currentUser.userId}&message=${encodeURIComponent(message)}&constitution=${encodeURIComponent(constitution)}`;

        console.log('SSE连接URL:', url);

        const eventSource = new EventSource(url);
        let fullReply = '';
        let isFirstChunk = true;

        eventSource.onmessage = (event) => {
            try {
                const rawData = event.data;
                let content = '';

                try {
                    const jsonData = JSON.parse(rawData);
                    if (jsonData.type === 'chunk' || jsonData.type === 'message') {
                        content = jsonData.content || jsonData.data || '';
                    } else if (jsonData.type === 'end' || jsonData.type === 'complete') {
                        eventSource.close();
                        saveChatToHistory(message, fullReply, startTime);
                        resolve();
                        return;
                    } else if (jsonData.type === 'error') {
                        eventSource.close();
                        reject(new Error(jsonData.content));
                        return;
                    } else if (jsonData.content) {
                        content = jsonData.content;
                    }
                } catch (e) {
                    content = rawData;
                    if (content === '[DONE]') {
                        eventSource.close();
                        saveChatToHistory(message, fullReply, startTime);
                        resolve();
                        return;
                    }
                }

                if (content && content.trim()) {
                    fullReply += content;
                    updateAIMessage(messageId, fullReply, startTime);
                    if (isFirstChunk) {
                        isFirstChunk = false;
                        removeLoadingAnimation(messageId);
                    }
                    forceScrollToBottom();
                }
            } catch (e) {
                console.error('处理SSE数据失败:', e);
            }
        };

        eventSource.onerror = (error) => {
            console.error('SSE连接错误:', error);
            eventSource.close();
            if (fullReply) {
                saveChatToHistory(message, fullReply, startTime);
                resolve();
            } else {
                reject(error);
            }
        };

        setTimeout(() => {
            if (eventSource.readyState !== EventSource.CLOSED) {
                eventSource.close();
                if (fullReply) {
                    saveChatToHistory(message, fullReply, startTime);
                    resolve();
                } else {
                    reject(new Error('请求超时'));
                }
            }
        }, 60000);
    });
}

// 使用普通HTTP接口
async function sendMessageWithHTTP(message, constitution, messageId, startTime) {
    try {
        const response = await fetch(`${API_BASE}/api/chat/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: window.currentUser.userId,
                message: message,
                constitution: constitution
            })
        });

        const data = await response.json();

        if (data.success) {
            removeLoadingAnimation(messageId);
            updateAIMessage(messageId, data.reply, startTime);
            saveChatToHistory(message, data.reply, startTime);
        } else {
            updateAIMessage(messageId, data.reply || '抱歉，我无法回复您的消息，请稍后重试。', startTime);
        }
    } catch (error) {
        console.error('HTTP请求失败:', error);
        updateAIMessage(messageId, '网络错误，请检查网络连接后重试。', startTime);
    }
}

// 强制滚动到底部
function forceScrollToBottom() {
    const container = document.getElementById('messagesContainer');
    if (container) {
        container.scrollTop = container.scrollHeight;
        console.log('滚动到底部，当前scrollTop:', container.scrollTop, 'scrollHeight:', container.scrollHeight);
    }
}

// 添加AI消息占位符
function addAIPlaceholder() {
    const container = document.getElementById('messagesContainer');
    if (!container) return null;

    const messageDiv = document.createElement('div');
    messageDiv.className = 'message ai-message';
    messageDiv.id = `ai-message-${Date.now()}`;

    messageDiv.innerHTML = `
        <div class="message-avatar">🤖</div>
        <div class="message-bubble">
            <div class="message-content">
                <div class="typing-indicator">
                    <span></span><span></span><span></span>
                </div>
            </div>
            <div class="message-time"></div>
        </div>
    `;

    container.appendChild(messageDiv);
    forceScrollToBottom();

    return messageDiv.id;
}

// 更新AI消息内容
function updateAIMessage(messageId, content, startTime) {
    const messageDiv = document.getElementById(messageId);
    if (!messageDiv) return;

    const contentDiv = messageDiv.querySelector('.message-content');
    const timeDiv = messageDiv.querySelector('.message-time');

    if (contentDiv) {
        const formattedContent = formatMessage(content);
        contentDiv.innerHTML = formattedContent;
    }

    if (timeDiv && startTime) {
        timeDiv.textContent = formatTimestamp(startTime);
    }

    forceScrollToBottom();
}

// 移除加载动画
function removeLoadingAnimation(messageId) {
    const messageDiv = document.getElementById(messageId);
    if (!messageDiv) return;

    const typingIndicator = messageDiv.querySelector('.typing-indicator');
    if (typingIndicator) {
        typingIndicator.remove();
    }
}

// 格式化时间戳
function formatTimestamp(timestamp) {
    if (!timestamp) return getCurrentTime();
    try {
        let date;
        if (typeof timestamp === 'string') {
            date = new Date(timestamp);
        } else if (timestamp instanceof Date) {
            date = timestamp;
        } else {
            date = new Date(timestamp);
        }
        if (isNaN(date.getTime())) {
            return getCurrentTime();
        }
        // 显示完整时间：月/日 时:分:秒
        return `${date.getMonth()+1}/${date.getDate()} ${date.toLocaleTimeString('zh-CN', { hour12: false })}`;
    } catch(e) {
        return getCurrentTime();
    }
}

// 添加消息到UI（添加滚动调用）
function addMessageToUI(content, isUser, timestamp = null) {
    const container = document.getElementById('messagesContainer');
    if (!container) {
        console.error('messagesContainer 不存在');
        return;
    }

    if (!content || !content.trim()) {
        console.warn('消息内容为空，跳过显示');
        return;
    }

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user-message' : 'ai-message'}`;

    const displayTime = formatTimestamp(timestamp);
    const formattedContent = isUser ? escapeHtml(content) : formatMessage(content);

    if (isUser) {
        messageDiv.innerHTML = `
            <div class="message-bubble">
                <div class="message-content">${formattedContent}</div>
                <div class="message-time">${displayTime}</div>
            </div>
            <div class="message-avatar">👤</div>
        `;
    } else {
        messageDiv.innerHTML = `
            <div class="message-avatar">🤖</div>
            <div class="message-bubble">
                <div class="message-content">${formattedContent}</div>
                <div class="message-time">${displayTime}</div>
            </div>
        `;
    }

    container.appendChild(messageDiv);
    // 每次添加消息后滚动到底部
    forceScrollToBottom();
}

// 格式化消息
function formatMessage(content) {
    if (!content) return '';

    let formatted = escapeHtml(content);
    formatted = formatted.replace(/^# (.+)$/gm, '<h3>$1</h3>');
    formatted = formatted.replace(/^## (.+)$/gm, '<h4>$1</h4>');
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    formatted = formatted.replace(/\*(.*?)\*/g, '<em>$1</em>');
    formatted = formatted.replace(/\n/g, '<br>');
    formatted = formatted.replace(/^[•·\-]\s+(.+)$/gm, '<li>$1</li>');
    formatted = formatted.replace(/^(\d+)\.\s+(.+)$/gm, '<li>$2</li>');

    if (formatted.includes('<li>')) {
        formatted = formatted.replace(/(<li>.*?<\/li>)\n(?=<li>)/g, '$1');
        formatted = '<ul class="message-list">' + formatted + '</ul>';
    }

    formatted = formatted.replace(/^&gt;\s+(.+)$/gm, '<blockquote>$1</blockquote>');

    return formatted;
}

// 保存聊天到历史
async function saveChatToHistory(userMessage, aiReply, timestamp = null) {
    const recordTime = timestamp || new Date();

    console.log('保存聊天记录，时间:', recordTime.toISOString());

    chatHistory.push({
        userMessage: userMessage,
        aiReply: aiReply,
        timestamp: recordTime.toISOString()
    });
    if (chatHistory.length > 100) chatHistory = chatHistory.slice(-100);

    try {
        const response = await fetch(`${API_BASE}/api/chat/save`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: window.currentUser.userId,
                userMessage: userMessage,
                aiReply: aiReply,
                timestamp: recordTime.toISOString()
            })
        });
        const result = await response.json();
        console.log('保存到后端结果:', result);
    } catch (error) {
        console.error('保存聊天记录到后端失败:', error);
    }
}

// 加载聊天历史
async function loadChatHistory() {
    if (isLoadingHistory) {
        console.log('正在加载历史记录，跳过');
        return;
    }

    if (!window.currentUser) {
        console.log('用户未登录，无法加载历史记录');
        return;
    }

    const container = document.getElementById('messagesContainer');
    if (!container) return;

    isLoadingHistory = true;

    try {
        console.log('开始加载聊天历史，userId:', window.currentUser.userId);

        const response = await fetch(`${API_BASE}/api/chat/history/${window.currentUser.userId}?limit=50`);

        if (!response.ok) {
            console.error('HTTP错误:', response.status);
            showWelcomeMessage();
            isLoadingHistory = false;
            return;
        }

        const data = await response.json();
        console.log('获取到的历史数据:', data);

        container.innerHTML = '';

        let historyList = [];

        if (Array.isArray(data)) {
            historyList = data;
        } else if (data && data.success && Array.isArray(data.records)) {
            historyList = data.records;
        } else if (data && data.records && Array.isArray(data.records)) {
            historyList = data.records;
        } else if (data && data.data && Array.isArray(data.data)) {
            historyList = data.data;
        } else {
            console.log('没有找到历史记录或格式不匹配');
        }

        if (historyList.length > 0) {
            console.log('显示历史记录，共', historyList.length, '条');
            console.log('第一条记录的结构:', historyList[0]);

            // 按时间排序
            historyList.sort((a, b) => {
                const timeA = a.timestamp || a.createTime || a.time || a.create_time;
                const timeB = b.timestamp || b.createTime || b.time || b.create_time;
                if (timeA && timeB) {
                    return new Date(timeA) - new Date(timeB);
                }
                return 0;
            });

            // 添加所有消息
            historyList.forEach((record) => {
                const recordTime = record.timestamp || record.createTime || record.time || record.create_time;

                // 尝试多种字段名获取消息内容
                const userMsg = record.userMessage || record.user_message || record.userMsg || record.user_msg || record.userContent || record.content;
                if (userMsg && userMsg.trim()) {
                    addMessageToUI(userMsg, true, recordTime);
                }

                const aiReply = record.aiReply || record.ai_reply || record.reply || record.aiResponse || record.assistantContent || record.response;
                if (aiReply && aiReply.trim()) {
                    addMessageToUI(aiReply, false, recordTime);
                }
            });

            // 使用多个延迟确保滚动生效
            setTimeout(() => {
                forceScrollToBottom();
                console.log('第一次滚动到底部');
            }, 100);

            setTimeout(() => {
                forceScrollToBottom();
                console.log('第二次滚动到底部');
            }, 300);

            setTimeout(() => {
                forceScrollToBottom();
                console.log('第三次滚动到底部');
                isLoadingHistory = false;
            }, 500);
        } else {
            console.log('没有历史记录，显示欢迎消息');
            showWelcomeMessage();
            isLoadingHistory = false;
        }

    } catch (error) {
        console.error('加载聊天历史失败:', error);
        showWelcomeMessage();
        isLoadingHistory = false;
    }
}

function showWelcomeMessage() {
    const container = document.getElementById('messagesContainer');
    if (!container) return;

    if (container.children.length > 0) return;

    const welcomeMessage = `👋 您好！我是您的食养AI助手。

**我能为您做什么？**
• 根据您的体质提供个性化养生建议
• 分析您的饮食记录并给出改善建议
• 解答中医养生问题
• 推荐适合您的食疗方案

**您可以这样问我：**
• "阴虚质适合吃什么？"
• "我最近睡眠不好怎么办？"
• "帮我分析一下今天的饮食"
• "推荐一个健脾祛湿的食谱"

请告诉我您的问题，我会尽力为您解答！`;

    addMessageToUI(welcomeMessage, false, new Date().toISOString());
}

function toggleInputState(enabled) {
    const inputElement = document.getElementById('messageInput');
    const sendButton = document.querySelector('.input-area button');

    if (inputElement) inputElement.disabled = !enabled;
    if (sendButton) {
        sendButton.disabled = !enabled;
        sendButton.textContent = enabled ? '发送' : '发送中...';
    }
}

async function clearChatHistory() {
    if (!confirm('确定要清空所有聊天记录吗？')) return;
    if (!window.currentUser) {
        alert('请先登录');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/api/chat/history/${window.currentUser.userId}`, {
            method: 'DELETE'
        });
        const data = await response.json();

        if (data.success) {
            chatHistory = [];
            const container = document.getElementById('messagesContainer');
            if (container) container.innerHTML = '';
            showWelcomeMessage();
            alert('聊天记录已清空');
        } else {
            alert('清空失败：' + (data.message || '未知错误'));
        }
    } catch (error) {
        console.error('清空聊天记录失败:', error);
        alert('网络错误，请稍后重试');
    }
}

function getCurrentTime() {
    const now = new Date();
    return now.toLocaleTimeString('zh-CN', { hour12: false });
}

function scrollToBottom() {
    forceScrollToBottom();
}

// 导出全局函数
window.sendMessage = sendMessage;
window.initChat = initChat;
window.clearChatHistory = clearChatHistory;

// 页面加载完成后初始化
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        setTimeout(() => {
            if (window.currentUser) initChat();
            else waitForUser().then(hasUser => hasUser && initChat());
        }, 500);
    });
} else {
    setTimeout(() => {
        if (window.currentUser) initChat();
        else waitForUser().then(hasUser => hasUser && initChat());
    }, 500);
}