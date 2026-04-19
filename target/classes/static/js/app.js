// 切换面板
function switchPanel(panelName) {
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    document.querySelector(`.nav-item[data-panel="${panelName}"]`).classList.add('active');

    document.querySelectorAll('.feature-panel').forEach(panel => panel.classList.remove('active'));
    document.getElementById(`${panelName}-panel`).classList.add('active');

    const inputArea = document.getElementById('inputArea');
    inputArea.style.display = panelName === 'chat' ? 'flex' : 'none';

    // 加载面板数据
    if (panelName === 'profile') { renderProfilePanel(); loadHealthProfile(); }
    if (panelName === 'diet') { renderDietPanel(); loadTodayRecords(); }
    if (panelName === 'assessment') renderAssessmentPanel();
    if (panelName === 'solar') { renderSolarPanel(); loadSolarInfo(); }
    if (panelName === 'report') { renderReportPanel(); loadDailyReport(); }
}

// 初始化
function init() {
    currentUser = checkAuth();
    if (!currentUser) return;

    document.getElementById('usernameDisplay').innerText = currentUser.username;
    document.getElementById('constitutionDisplay').innerText = currentUser.constitution || '未测评';
    document.getElementById('avatarEmoji').innerText = getAvatarByConstitution(currentUser.constitution);
    document.getElementById('userInfoBtn').addEventListener('click', showLogoutPopup);

    loadChatHistory();
    switchPanel('chat');
}

// 将函数挂载到window，供onclick调用
window.switchPanel = switchPanel;
window.sendMessage = sendMessage;
window.toggleSidebar = toggleSidebar;
window.selectMeal = selectMeal;
window.addDietRecord = addDietRecord;
window.saveHealthProfile = saveHealthProfile;
window.addTag = addTag;
window.removeTag = removeTag;
window.submitAssessment = submitAssessment;
window.refreshReport = refreshReport;

// 启动
init();