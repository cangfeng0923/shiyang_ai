// app.js - 简化修复版

// 切换面板
function switchPanel(panelName) {
    console.log('切换到面板:', panelName);

    // 移除所有激活状态
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    document.querySelectorAll('.feature-panel').forEach(panel => panel.classList.remove('active'));

    // 激活当前导航项
    const navItem = document.querySelector(`.nav-item[data-panel="${panelName}"]`);
    if (navItem) {
        navItem.classList.add('active');
    }

    // 激活当前面板
    const panel = document.getElementById(`${panelName}-panel`);
    if (panel) {
        panel.classList.add('active');
    }

    // 特殊处理输入区域显示
    const inputArea = document.getElementById('inputArea');
    if (inputArea) {
        inputArea.style.display = panelName === 'chat' ? 'flex' : 'none';
    }

    // 聊天面板：滚动到底部
    if (panelName === 'chat') {
        setTimeout(() => {
            if (typeof scrollToBottom === 'function') {
                scrollToBottom();
            }
        }, 100);
    }

    // 加载面板数据
    switch (panelName) {
        case 'profile':
            if (typeof renderProfilePanel === 'function') renderProfilePanel();
            break;
        case 'diet':
            if (typeof renderDietPanel === 'function') renderDietPanel();
            break;
        case 'sleep':
            if (typeof renderSleepPanel === 'function') renderSleepPanel();
            break;
        case 'assessment':
            if (typeof renderAssessmentPanel === 'function') renderAssessmentPanel();
            break;
        case 'solar':
            if (typeof renderSolarPanel === 'function') renderSolarPanel();
            if (typeof loadSolarInfo === 'function') loadSolarInfo();
            break;
        case 'report':
            if (typeof renderReportPanel === 'function') renderReportPanel();
            if (typeof loadSevenDayReport === 'function') loadSevenDayReport();
            break;
        default:
            break;
    }
}

// 绑定导航菜单点击事件
function bindNavEvents() {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.removeEventListener('click', handleNavClick);
        item.addEventListener('click', handleNavClick);
    });
}

function handleNavClick(e) {
    const panel = this.getAttribute('data-panel');
    if (panel && typeof switchPanel === 'function') {
        switchPanel(panel);
    }
}

// 初始化
async function init() {
    console.log('初始化开始...');
    currentUser = checkAuth();
    if (!currentUser) return;

    // 显示用户信息
    const usernameDisplay = document.getElementById('usernameDisplay');
    const constitutionDisplay = document.getElementById('constitutionDisplay');
    const avatarEmoji = document.getElementById('avatarEmoji');
    const userInfoBtn = document.getElementById('userInfoBtn');

    if (usernameDisplay) usernameDisplay.innerText = currentUser.username;
    if (constitutionDisplay) constitutionDisplay.innerText = currentUser.constitution || '未测评';
    if (avatarEmoji) avatarEmoji.innerText = getAvatarByConstitution(currentUser.constitution);
    if (userInfoBtn) {
        userInfoBtn.removeEventListener('click', showLogoutPopup);
        userInfoBtn.addEventListener('click', showLogoutPopup);
    }

    // 绑定导航事件
    bindNavEvents();

    // 先切换到聊天面板
    switchPanel('chat');

    // 再加载历史记录
    if (typeof loadChatHistory === 'function') {
        await loadChatHistory();
    }

    console.log('初始化完成');
}

// 挂载全局函数
window.switchPanel = switchPanel;

// 启动
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}