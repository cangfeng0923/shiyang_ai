function renderReportPanel() {
    const panel = document.getElementById('report-panel');
    panel.innerHTML = `
        <div class="form-card" id="reportContent">加载中...</div>
        <button class="btn-primary" onclick="refreshReport()">🔄 刷新报告</button>
    `;
}

async function loadDailyReport() {
    if (!currentUser) return;
    try {
        const response = await fetch(`${API_BASE}/api/diet/report/${currentUser.userId}?constitution=${currentUser.constitution || '平和质'}`);
        const data = await response.json();
        const container = document.getElementById('reportContent');
        if (data.report && data.report.totalRecords > 0) {
            const report = data.report;
            container.innerHTML = `<h4>📊 今日饮食报告</h4>
                <p>📅 ${report.date}</p><p>🍽️ 记录餐数：${report.totalRecords} 餐</p>
                <p>🔥 估算热量：${report.totalCalories} 千卡</p>
                <p>⭐ 健康评分：${report.avgHealthScore}/100</p>
                <p>💡 ${report.advice || '保持良好饮食习惯'}</p>`;
        } else container.innerHTML = '<p>暂无今日报告，请先记录饮食</p>';
    } catch (error) {
        document.getElementById('reportContent').innerHTML = '<p>加载失败</p>';
    }
}

async function refreshReport() {
    await loadDailyReport();
    alert('报告已刷新');
}