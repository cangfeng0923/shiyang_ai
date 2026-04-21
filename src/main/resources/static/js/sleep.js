// sleep.js - 睡眠功能模块（修复版）

// 确保 API_BASE 已定义
if (typeof API_BASE === 'undefined') {
    var API_BASE = '';  // 使用相对路径
    // 或者 var API_BASE = 'http://localhost:8080';
}

let currentSleepDate = new Date().toISOString().split('T')[0];

// 渲染睡眠面板
function renderSleepPanel() {
    const panel = document.getElementById('sleep-panel');
    if (!panel) return;

    panel.innerHTML = `
        <div class="form-card">
            <h4>😴 记录今日睡眠</h4>
            <div class="form-row">
                <label>记录日期</label>
                <input type="date" id="sleepDate" value="${currentSleepDate}">
            </div>
            <div class="form-row">
                <label>就寝时间</label>
                <input type="time" id="bedtime" placeholder="例如: 22:30">
            </div>
            <div class="form-row">
                <label>起床时间</label>
                <input type="time" id="wakeupTime" placeholder="例如: 07:00">
            </div>
            <div class="form-row">
                <label>睡眠质量</label>
                <select id="sleepQuality">
                    <option value="">请选择</option>
                    <option value="EXCELLENT">优秀</option>
                    <option value="GOOD">良好</option>
                    <option value="FAIR">一般</option>
                    <option value="POOR">较差</option>
                </select>
            </div>
            <div class="form-row">
                <label>睡眠时长 (小时)</label>
                <input type="number" id="sleepDuration" step="0.1" placeholder="例如: 8.5">
            </div>
            <div class="form-row">
                <label>深睡时长 (分钟)</label>
                <input type="number" id="deepSleepMinutes" placeholder="例如: 120">
            </div>
            <div class="form-row">
                <label>醒来次数</label>
                <input type="number" id="wakeCount" placeholder="例如: 1">
            </div>
            <div class="form-row">
                <label>备注</label>
                <textarea id="sleepNotes" rows="2" placeholder="睡眠感受、梦境等（可选）" style="width:100%; padding:10px; border-radius:10px; border:1px solid #e0e4e8; resize:none;"></textarea>
            </div>
            <button class="btn-primary" onclick="addSleepRecord()">💤 记录睡眠</button>
        </div>
        <div class="form-card">
            <h4>📊 今日睡眠</h4>
            <div id="todaySleepRecords" class="record-list"></div>
        </div>
        <div class="form-card">
            <h4>📈 本周睡眠报告</h4>
            <div id="sleepReport" class="report-content">加载中...</div>
            <button class="btn-primary" onclick="loadSleepReport()" style="margin-top: 12px;">🔄 刷新报告</button>
        </div>
    `;

    // 绑定日期选择事件
    const dateInput = document.getElementById('sleepDate');
    if (dateInput) {
        dateInput.onchange = function() {
            currentSleepDate = this.value;
        };
    }

    // 加载数据
    loadTodaySleepRecords();
    loadSleepReport();
}

// 添加睡眠记录
async function addSleepRecord() {
    if (!currentUser) {
        alert('请先登录');
        return;
    }

    const bedtime = document.getElementById('bedtime').value;
    const wakeupTime = document.getElementById('wakeupTime').value;
    const sleepDuration = parseFloat(document.getElementById('sleepDuration').value);
    const deepSleepMinutes = parseInt(document.getElementById('deepSleepMinutes').value);
    const wakeCount = parseInt(document.getElementById('wakeCount').value);
    const quality = document.getElementById('sleepQuality').value;
    const notes = document.getElementById('sleepNotes').value;

    // 验证必填字段
    if (!bedtime || !wakeupTime) {
        alert('请填写就寝时间和起床时间');
        return;
    }

    const record = {
        userId: currentUser.userId,
        recordDate: document.getElementById('sleepDate').value || currentSleepDate,
        bedtime: bedtime + ':00',
        wakeupTime: wakeupTime + ':00',
        quality: quality || 'FAIR',
        sleepDuration: isNaN(sleepDuration) ? null : sleepDuration,
        deepSleepMinutes: isNaN(deepSleepMinutes) ? null : deepSleepMinutes,
        wakeCount: isNaN(wakeCount) ? null : wakeCount,
        notes: notes || null
    };

    try {
        // 使用 API_BASE，如果未定义则使用空字符串
        const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';
        const response = await fetch(`${baseUrl}/api/sleep/record`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(record)
        });
        const data = await response.json();
        if (data.success) {
            alert(`睡眠记录成功！睡眠评分：${data.sleepScore}/100`);
            // 清空表单
            document.getElementById('bedtime').value = '';
            document.getElementById('wakeupTime').value = '';
            document.getElementById('sleepQuality').value = '';
            document.getElementById('sleepDuration').value = '';
            document.getElementById('deepSleepMinutes').value = '';
            document.getElementById('wakeCount').value = '';
            document.getElementById('sleepNotes').value = '';
            // 刷新数据
            await loadTodaySleepRecords();
            await loadSleepReport();
        } else {
            alert('记录失败: ' + (data.message || '未知错误'));
        }
    } catch (error) {
        console.error('记录睡眠失败:', error);
        alert('网络错误: ' + error.message);
    }
}

// 加载今日睡眠记录
async function loadTodaySleepRecords() {
    if (!currentUser) return;
    const container = document.getElementById('todaySleepRecords');
    if (!container) return;

    try {
        const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';
        const response = await fetch(`${baseUrl}/api/sleep/today/${currentUser.userId}`);
        const data = await response.json();

        if (data.records && data.records.length > 0) {
            container.innerHTML = data.records.map(record => {
                let qualityText = '未知';
                let qualityClass = '';
                switch(record.quality) {
                    case 'EXCELLENT': qualityText = '优秀'; qualityClass = 'score-excellent'; break;
                    case 'GOOD': qualityText = '良好'; qualityClass = 'score-good'; break;
                    case 'FAIR': qualityText = '一般'; qualityClass = 'score-fair'; break;
                    case 'POOR': qualityText = '较差'; qualityClass = 'score-poor'; break;
                }

                let sleepScore = '--';
                if (record.sleepDuration) {
                    if (record.sleepDuration >= 7 && record.sleepDuration <= 9) sleepScore = '优秀';
                    else if (record.sleepDuration >= 6) sleepScore = '良好';
                    else sleepScore = '不足';
                }

                return `<div class="record-item">
                    <div style="flex:1">
                        <strong>📅 ${record.recordDate}</strong><br>
                        🕐 ${record.bedtime || '?'} - ${record.wakeupTime || '?'}<br>
                        ⏱️ 时长: ${record.sleepDuration || '?'}小时<br>
                        💤 质量: ${qualityText}<br>
                        ${record.notes ? '📝 ' + escapeHtml(record.notes) : ''}
                    </div>
                    <div class="${qualityClass}" style="font-weight:bold; text-align:center; min-width:50px;">
                        ${sleepScore}
                    </div>
                </div>`;
            }).join('');
        } else {
            container.innerHTML = '<p style="color:#999; text-align:center; padding:20px;">今日暂无睡眠记录</p>';
        }
    } catch (error) {
        console.error('加载睡眠记录失败:', error);
        container.innerHTML = '<p style="color:#999; text-align:center;">加载失败</p>';
    }
}

// 加载睡眠报告
async function loadSleepReport() {
    if (!currentUser) return;
    const container = document.getElementById('sleepReport');
    if (!container) return;

    container.innerHTML = '<div class="loading"></div> 加载中...';

    try {
        const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';
        const response = await fetch(`${baseUrl}/api/sleep/report/${currentUser.userId}`);
        const data = await response.json();
        if (data.report) {
            let formattedReport = data.report
                .replace(/### /g, '<strong>')
                .replace(/\n/g, '<br>')
                .replace(/- /g, '• ');
            container.innerHTML = `<div style="line-height:1.8;">${formattedReport}</div>`;
        } else {
            container.innerHTML = '<p>暂无睡眠报告，请先记录睡眠数据</p>';
        }
    } catch (error) {
        console.error('加载睡眠报告失败:', error);
        container.innerHTML = '<p>加载失败，请稍后重试</p>';
    }
}