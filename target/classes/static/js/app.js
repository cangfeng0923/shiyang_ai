// app.js - 确保所有函数正确挂载到 window

// 检查登录状态
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

// 退出登录
function logout() {
    localStorage.removeItem('currentUser');
    window.location.href = 'index.html';
}

// 显示退出弹窗
function showLogoutPopup(event) {
    const rect = event.currentTarget.getBoundingClientRect();
    const existingPopup = document.querySelector('.popup-menu');
    if (existingPopup) existingPopup.remove();

    const popup = document.createElement('div');
    popup.className = 'popup-menu';
    popup.style.left = rect.left + 'px';
    popup.style.bottom = (window.innerHeight - rect.top + 8) + 'px';
    popup.innerHTML = `<div class="popup-menu-item logout" onclick="logout()"><span>🚪</span> 退出登录</div>`;
    document.body.appendChild(popup);

    const closePopup = (e) => {
        if (!popup.contains(e.target)) popup.remove();
        document.removeEventListener('click', closePopup);
    };
    setTimeout(() => document.addEventListener('click', closePopup), 0);
}

// 切换侧边栏
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (window.innerWidth <= 768) {
        sidebar.classList.toggle('open');
    } else {
        sidebar.classList.toggle('collapsed');
    }
}

// 切换面板
function switchPanel(panelName) {
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

    // 加载面板数据
    switch (panelName) {
        case 'profile':
            if (typeof renderProfilePanel === 'function') renderProfilePanel();
            if (typeof loadHealthProfile === 'function') loadHealthProfile();
            break;
        case 'diet':
            if (typeof renderDietPanel === 'function') renderDietPanel();
            if (typeof loadTodayRecords === 'function') loadTodayRecords();
            if (typeof loadWeekReport === 'function') loadWeekReport();
            break;
        case 'sleep':
            if (typeof renderSleepPanel === 'function') renderSleepPanel();
            if (typeof loadTodaySleepRecords === 'function') loadTodaySleepRecords();
            if (typeof loadSleepReport === 'function') loadSleepReport();
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
            if (typeof loadFusionReport === 'function') loadFusionReport();  // 改为 loadFusionReport
            break;
        default:
            if (panelName === 'chat' && typeof loadChatHistory === 'function') {
                loadChatHistory();
            }
    }
}

// 挂载全局函数
window.switchPanel = switchPanel;
window.toggleSidebar = toggleSidebar;
window.showLogoutPopup = showLogoutPopup;
window.logout = logout;

// 其他函数会在各模块加载时挂载

// 初始化
function init() {
    currentUser = checkAuth();
    if (!currentUser) return;

    const usernameDisplay = document.getElementById('usernameDisplay');
    const constitutionDisplay = document.getElementById('constitutionDisplay');
    const avatarEmoji = document.getElementById('avatarEmoji');
    const userInfoBtn = document.getElementById('userInfoBtn');

    if (usernameDisplay) usernameDisplay.innerText = currentUser.username;
    if (constitutionDisplay) constitutionDisplay.innerText = currentUser.constitution || '未测评';
    if (avatarEmoji) avatarEmoji.innerText = getAvatarByConstitution ? getAvatarByConstitution(currentUser.constitution) : '👤';
    if (userInfoBtn) userInfoBtn.addEventListener('click', showLogoutPopup);

    if (typeof loadChatHistory === 'function') loadChatHistory();
    switchPanel('chat');
}

// 启动
init();