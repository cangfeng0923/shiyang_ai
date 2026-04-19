// 发送消息
async function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value.trim();
    if (!message || !currentUser) return;

    addUserMessage(message);
    input.value = '';

    const loadingMsg = addLoadingMessage();

    try {
        const response = await fetch(`${API_BASE}/api/chat/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.userId, message: message })
        });
        const data = await response.json();
        loadingMsg.remove();
        addAIMessage(data.reply || '抱歉，服务暂时不可用');
    } catch (error) {
        loadingMsg.remove();
        addAIMessage('网络错误，请稍后再试');
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
    const div = document.createElement('div');
    div.className = 'message ai';
    div.innerHTML = `<div class="message-avatar">🌿</div><div class="message-content">${escapeHtml(content).replace(/\n/g, '<br>')}</div>`;
    container.appendChild(div);
    scrollToBottom();
}

function addLoadingMessage() {
    const container = document.getElementById('messagesContainer');
    const div = document.createElement('div');
    div.className = 'message ai';
    div.id = 'loadingMsg';
    div.innerHTML = `<div class="message-avatar">🌿</div><div class="message-content"><div class="loading"></div> 思考中...</div>`;
    container.appendChild(div);
    scrollToBottom();
    return div;
}

async function loadChatHistory() {
    if (!currentUser) return;
    try {
        const response = await fetch(`${API_BASE}/api/chat/history/${currentUser.userId}?limit=50`);
        const history = await response.json();
        const container = document.getElementById('messagesContainer');
        container.innerHTML = '';
        if (history && history.length > 0) {
            history.forEach(chat => {
                if (chat.role === 'user') addUserMessage(chat.content);
                else if (chat.role === 'assistant' || chat.role === 'ai') addAIMessage(chat.content);
            });
        } else {
            addAIMessage(`您好${currentUser.username}！我是您的专属中医顾问。${currentUser.constitution ? '您是' + currentUser.constitution + '体质' : '请完成体质测评'}，有什么健康问题可以问我。`);
        }
    } catch (error) {
        addAIMessage(`您好${currentUser.username}！有什么健康问题可以问我。`);
    }
}