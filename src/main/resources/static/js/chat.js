// chat.js - 真正的逐字打字机效果版

// 全局变量
let currentStreamController = null; // 用于控制当前流式输出

// 发送消息（流式输出）
async function sendMessage() {
    const input = document.getElementById('messageInput');
    const message = input.value.trim();
    if (!message || !currentUser) return;

    addUserMessage(message);
    input.value = '';

    // 取消之前的流式输出
    if (currentStreamController) {
        clearTimeout(currentStreamController.timeout);
        currentStreamController = null;
    }

    // 添加一个空的AI消息占位符，并显示"思考中..."
    const aiMessageDiv = addAIMessagePlaceholder();
    updateAIMessage(aiMessageDiv, '思考中...');

    let fullReply = '';
    let eventSource = null;
    let charQueue = []; // 字符队列
    let isTyping = false; // 是否正在打字
    let currentDisplayText = ''; // 当前已显示的文本

    // 逐字显示函数
    const typeNextChar = () => {
        if (!currentStreamController) return;

        if (charQueue.length > 0) {
            const nextChar = charQueue.shift();
            currentDisplayText += nextChar;
            updateAIMessage(aiMessageDiv, currentDisplayText);
            scrollToBottom();

            // 随机延迟 30-80ms 模拟真实打字效果
            const delay = Math.random() * 50 + 30;
            currentStreamController.timeout = setTimeout(typeNextChar, delay);
            isTyping = true;
        } else {
            isTyping = false;
            // 等待新数据
            if (currentStreamController) {
                currentStreamController.timeout = setTimeout(() => {
                    if (!isTyping && charQueue.length === 0 && currentStreamController) {
                        // 没有新数据，认为完成
                        if (currentStreamController.completed) {
                            finalizeAIMessage(aiMessageDiv);
                        }
                    }
                }, 500);
            }
        }
    };

    // 添加新数据到队列
    const addToQueue = (newContent) => {
        if (!newContent) return;

        // 将新内容拆分成单个字符（包括中文、英文、标点）
        const chars = newContent.split('');
        for (const char of chars) {
            charQueue.push(char);
        }

        // 如果当前没有在打字，开始打字
        if (!isTyping && currentStreamController) {
            if (currentStreamController.timeout) {
                clearTimeout(currentStreamController.timeout);
            }
            typeNextChar();
        }
    };

    currentStreamController = {
        completed: false,
        timeout: null
    };

    try {
        const url = `${API_BASE}/api/chat/stream?userId=${currentUser.userId}&message=${encodeURIComponent(message)}`;
        console.log('流式请求URL:', url);

        eventSource = new EventSource(url);

        eventSource.onopen = function() {
            console.log('SSE连接已打开');
        };

        // 监听 message 事件
        eventSource.addEventListener('message', function(event) {
            console.log('收到数据块:', event.data);

            if (event.data === '[DONE]') {
                return;
            }

            if (event.data && event.data !== '') {
                fullReply += event.data;
                // 将数据块加入队列进行逐字显示
                addToQueue(event.data);
            }
        });

        // 监听 complete 事件
        eventSource.addEventListener('complete', function(event) {
            console.log('流式对话完成');
            eventSource.close();

            if (currentStreamController) {
                currentStreamController.completed = true;
                // 等待队列中的字符显示完毕
                const checkQueue = () => {
                    if (charQueue.length === 0 && !isTyping) {
                        finalizeAIMessage(aiMessageDiv);
                        currentStreamController = null;
                    } else {
                        setTimeout(checkQueue, 100);
                    }
                };
                checkQueue();
            }

            if (!fullReply) {
                updateAIMessage(aiMessageDiv, '抱歉，没有收到回复，请稍后再试。');
                currentStreamController = null;
            }
        });

        // 监听 error 事件
        eventSource.addEventListener('error', function(event) {
            console.error('SSE错误:', event);
            if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
                eventSource.close();
            }
            if (!fullReply) {
                updateAIMessage(aiMessageDiv, '抱歉，服务出现异常，请稍后再试。');
            }
            currentStreamController = null;
        });

        // 设置超时
        setTimeout(() => {
            if (eventSource && eventSource.readyState !== EventSource.CLOSED) {
                eventSource.close();
                if (!fullReply) {
                    updateAIMessage(aiMessageDiv, '请求超时，请稍后再试。');
                }
                currentStreamController = null;
            }
        }, 60000);

    } catch (error) {
        console.error('发送消息错误:', error);
        if (eventSource) eventSource.close();
        updateAIMessage(aiMessageDiv, '网络错误，请稍后再试。');
        currentStreamController = null;
    }
}

// 添加AI消息占位符
function addAIMessagePlaceholder() {
    const container = document.getElementById('messagesContainer');
    const div = document.createElement('div');
    div.className = 'message ai';
    div.innerHTML = `<div class="message-avatar">🌿</div><div class="message-content"></div>`;
    container.appendChild(div);
    scrollToBottom();
    return div;
}

// 更新AI消息内容
function updateAIMessage(messageDiv, content) {
    const contentDiv = messageDiv.querySelector('.message-content');
    if (contentDiv) {
        if (content === '思考中...') {
            contentDiv.innerHTML = '<span class="thinking">思考中<span class="dots">...</span></span>';
            return;
        }

        // 显示内容并添加光标
        const formattedContent = escapeHtml(content).replace(/\n/g, '<br>');
        let cleanContent = formattedContent.replace(/<span class="typing-cursor">\|<\/span>/g, '');
        contentDiv.innerHTML = cleanContent + '<span class="typing-cursor">|</span>';
        scrollToBottom();
    }
}

// 完成消息后移除光标
function finalizeAIMessage(messageDiv) {
    const contentDiv = messageDiv.querySelector('.message-content');
    if (contentDiv) {
        contentDiv.innerHTML = contentDiv.innerHTML.replace('<span class="typing-cursor">|</span>', '');
    }
}

// 添加用户消息
function addUserMessage(content) {
    const container = document.getElementById('messagesContainer');
    const div = document.createElement('div');
    div.className = 'message user';
    div.innerHTML = `<div class="message-content">${escapeHtml(content)}</div><div class="message-avatar">👤</div>`;
    container.appendChild(div);
    scrollToBottom();
}

// 添加普通AI消息（用于历史加载）
function addAIMessage(content) {
    const container = document.getElementById('messagesContainer');
    const div = document.createElement('div');
    div.className = 'message ai';
    div.innerHTML = `<div class="message-avatar">🌿</div><div class="message-content">${escapeHtml(content).replace(/\n/g, '<br>')}</div>`;
    container.appendChild(div);
    scrollToBottom();
}

// 加载历史消息
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
        console.error('加载历史失败:', error);
        addAIMessage(`您好${currentUser.username}！有什么健康问题可以问我。`);
    }
}

// 辅助函数
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    if (container) {
        container.scrollTop = container.scrollHeight;
    }
}