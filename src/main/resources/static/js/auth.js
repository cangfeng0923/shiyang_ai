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