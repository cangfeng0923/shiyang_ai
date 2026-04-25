// chat.js - 简化版

// 全局变量
let currentStreamController = null;

// 发送消息（流式输出）
async function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value.trim();
    if (!message || !currentUser) return;

    // 添加用户消息
    addUserMessage(message);
    input.value = '';

    // 取消之前的流式输出
    if (currentStreamController) {
        if (currentStreamController.eventSource) {
            currentStreamController.eventSource.close();
        }
        currentStreamController = null;
    }

    // 添加AI消息占位符
    const aiMessageDiv = addAIMessagePlaceholder();
    let fullContent = '';
    let isFirstChunk = true;

    try {
        const url = `${API_BASE}/api/chat/stream?userId=${currentUser.userId}&message=${encodeURIComponent(message)}`;
        const eventSource = new EventSource(url);

        currentStreamController = { eventSource: eventSource };

        // 监听 message 事件
        eventSource.addEventListener('message', (event) => {
            try {
                const data = event.data;
                if (data === '[DONE]') {
                    return;
                }
                if (data && data !== '') {
                    fullContent += data;
                    // 更新消息内容
                    const contentDiv = aiMessageDiv.querySelector('.message-content');
                    if (contentDiv) {
                        // 移除光标
                        let cleanContent = fullContent.replace(/\n/g, '<br>');
                        // 处理 markdown 加粗
                        cleanContent = cleanContent.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                        contentDiv.innerHTML = cleanContent + '<span class="typing-cursor"></span>';
                        scrollToBottom();
                    }
                }
            } catch (err) {
                console.error('处理消息失败:', err);
            }
        });

        // 监听 complete 事件
        eventSource.addEventListener('complete', () => {
            // 移除光标
            const contentDiv = aiMessageDiv.querySelector('.message-content');
            if (contentDiv) {
                let finalContent = fullContent.replace(/\n/g, '<br>');
                finalContent = finalContent.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                contentDiv.innerHTML = finalContent;
            }
            eventSource.close();
            currentStreamController = null;
            scrollToBottom();
        });

        // 监听 error 事件
        eventSource.addEventListener('error', (err) => {
            console.error('SSE错误:', err);
            if (!fullContent) {
                const contentDiv = aiMessageDiv.querySelector('.message-content');
                if (contentDiv) {
                    contentDiv.innerHTML = '抱歉，服务出现异常，请稍后再试。';
                }
            } else {
                // 已经有内容，只移除光标
                const contentDiv = aiMessageDiv.querySelector('.message-content');
                if (contentDiv) {
                    let finalContent = fullContent.replace(/\n/g, '<br>');
                    finalContent = finalContent.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                    contentDiv.innerHTML = finalContent;
                }
            }
            eventSource.close();
            currentStreamController = null;
            scrollToBottom();
        });

        // 设置超时（60秒）
        setTimeout(() => {
            if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
                console.log('流式请求超时');
                eventSource.close();
                if (!fullContent) {
                    const contentDiv = aiMessageDiv.querySelector('.message-content');
                    if (contentDiv) {
                        contentDiv.innerHTML = '请求超时，请稍后再试。';
                    }
                }
                currentStreamController = null;
                scrollToBottom();
            }
        }, 60000);

    } catch (error) {
        console.error('发送消息错误:', error);
        const contentDiv = aiMessageDiv.querySelector('.message-content');
        if (contentDiv) {
            contentDiv.innerHTML = '网络错误，请稍后再试。';
        }
        currentStreamController = null;
        scrollToBottom();
    }
}

function addAIMessagePlaceholder() {
    const container = document.getElementById('messagesContainer');
    const div = document.createElement('div');
    div.className = 'message ai';
    div.innerHTML = `<div class="message-avatar">🌿</div><div class="message-content"><span class="thinking">思考中...</span></div>`;
    container.appendChild(div);
    scrollToBottom();
    return div;
}

function updateAIMessage(messageDiv, content) {
    const contentDiv = messageDiv.querySelector('.message-content');
    if (contentDiv) {
        if (content === '思考中...') {
            contentDiv.innerHTML = '<span class="thinking">思考中<span class="dots">...</span></span>';
            return;
        }
        const formattedContent = escapeHtml(content).replace(/\n/g, '<br>');
        let cleanContent = formattedContent.replace(/<span class="typing-cursor">[^<]*<\/span>/g, '');
        contentDiv.innerHTML = cleanContent + '<span class="typing-cursor"></span>';
        scrollToBottom();
    }
}

function finalizeAIMessage(messageDiv) {
    const contentDiv = messageDiv.querySelector('.message-content');
    if (contentDiv) {
        contentDiv.innerHTML = contentDiv.innerHTML.replace(/<span class="typing-cursor"><\/span>/g, '');
        contentDiv.innerHTML = contentDiv.innerHTML.replace(/<span class="typing-cursor">[^<]*<\/span>/g, '');
    }
}

function addUserMessage(content) {
    const container = document.getElementById('messagesContainer');
    const div = document.createElement('div');
    div.className = 'message user';
    div.innerHTML = `<div class="message-content">${escapeHtml(content)}</div><div class="message-avatar">👤</div>`;
    container.appendChild(div);
    scrollToBottom();
}

function addAIMessage(content) {
    const container = document.getElementById('messagesContainer');
    if (!container) return;
    const div = document.createElement('div');
    div.className = 'message ai';
    div.innerHTML = `<div class="message-avatar">🌿</div><div class="message-content">${escapeHtml(content).replace(/\n/g, '<br>')}</div>`;
    container.appendChild(div);
}

// 加载历史消息
async function loadChatHistory() {
    if (!currentUser) return;

    const container = document.getElementById('messagesContainer');
    if (!container) return;

    container.innerHTML = '';

    try {
        const response = await fetch(`${API_BASE}/api/chat/history/${currentUser.userId}?limit=50`);
        const history = await response.json();

        if (history && history.length > 0) {
            history.forEach(chat => {
                if (chat.role === 'user') {
                    addUserMessage(chat.content);
                } else if (chat.role === 'assistant' || chat.role === 'ai') {
                    addAIMessage(chat.content);
                }
            });
        } else {
            const welcomeMsg = `您好${currentUser.username}！我是您的专属中医顾问。${currentUser.constitution ? '您是' + currentUser.constitution + '体质' : '请完成体质测评'}，有什么健康问题可以问我。`;
            addAIMessage(welcomeMsg);
        }

        // 滚动到底部
        setTimeout(() => {
            scrollToBottom();
        }, 100);

    } catch (error) {
        console.error('加载历史失败:', error);
        const welcomeMsg = `您好${currentUser.username}！有什么健康问题可以问我。`;
        addAIMessage(welcomeMsg);
        scrollToBottom();
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function scrollToBottom() {
    // 滚动 chat-panel（父容器），不是 messagesContainer
    const panel = document.getElementById('chat-panel');
    if (panel) {
        panel.scrollTop = panel.scrollHeight;
    }
}

// 确保函数挂载到 window
window.sendMessage = sendMessage;
window.loadChatHistory = loadChatHistory;
window.scrollToBottom = scrollToBottom;