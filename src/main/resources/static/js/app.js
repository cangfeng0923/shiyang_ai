// app.js - 完整版

function checkAuth() {
    const savedUser = localStorage.getItem('currentUser');
    if (!savedUser) {
        window.location.href = 'index.html';
        return null;
    }
    try {
        return JSON.parse(savedUser);
    } catch(e) {
        window.location.href = 'index.html';
        return null;
    }
}

function logout() {
    localStorage.removeItem('currentUser');
    window.location.href = 'index.html';
}

function showLogoutPopup(event) {
    event.stopPropagation();
    const rect = event.currentTarget.getBoundingClientRect();
    const existingPopup = document.querySelector('.popup-menu');
    if (existingPopup) existingPopup.remove();

    const popup = document.createElement('div');
    popup.className = 'popup-menu';
    popup.style.left = rect.left + 'px';
    popup.style.bottom = (window.innerHeight - rect.top + 8) + 'px';
    popup.innerHTML = `<div class="popup-menu-item logout" onclick="window.logout()"><span>🚪</span> 退出登录</div>`;
    document.body.appendChild(popup);

    const closePopup = (e) => {
        if (!popup.contains(e.target)) popup.remove();
        document.removeEventListener('click', closePopup);
    };
    setTimeout(() => document.addEventListener('click', closePopup), 0);
}

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (window.innerWidth <= 768) {
        sidebar.classList.toggle('open');
    } else {
        sidebar.classList.toggle('collapsed');
    }
}

function switchPanel(panelName) {
    console.log('切换到面板:', panelName);

    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    document.querySelectorAll('.feature-panel').forEach(panel => panel.classList.remove('active'));

    const navItem = document.querySelector(`.nav-item[data-panel="${panelName}"]`);
    if (navItem) navItem.classList.add('active');

    const panel = document.getElementById(`${panelName}-panel`);
    if (panel) panel.classList.add('active');

    const inputArea = document.getElementById('inputArea');
    if (inputArea) inputArea.style.display = panelName === 'chat' ? 'flex' : 'none';

    if (panelName === 'chat') {
        setTimeout(() => {
            if (typeof scrollToBottom === 'function') scrollToBottom();
        }, 100);
    }

    // 加载面板数据
    switch (panelName) {
        case 'diet':
            if (typeof renderDietPanel === 'function') renderDietPanel();
            break;
        case 'profile':
            if (typeof renderProfilePanel === 'function') renderProfilePanel();
            break;
        case 'sleep':
            if (typeof renderSleepPanel === 'function') renderSleepPanel();
            break;
        case 'assessment':
            if (typeof renderAssessmentPanel === 'function') renderAssessmentPanel();
            break;
        case 'solar':
            if (typeof renderSolarPanel === 'function') renderSolarPanel();
            break;
        case 'report':
            if (typeof renderReportPanel === 'function') renderReportPanel();
            break;
        default:
            break;
    }
}

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

async function init() {
    console.log('初始化开始...');
    currentUser = checkAuth();
    if (!currentUser) return;

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

    bindNavEvents();
    switchPanel('chat');

    if (typeof loadChatHistory === 'function') {
        await loadChatHistory();
    }
    console.log('初始化完成');
}

window.switchPanel = switchPanel;
window.toggleSidebar = toggleSidebar;
window.logout = logout;
window.showLogoutPopup = showLogoutPopup;

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}