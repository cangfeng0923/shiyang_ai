// sleep.js - 简化版睡眠功能模块

let currentSleepDate = new Date().toISOString().split('T')[0];
let editingSleepId = null;

// 渲染睡眠面板（简化版）
function renderSleepPanel() {
    const panel = document.getElementById('sleep-panel');
    if (!panel) return;

    panel.innerHTML = `
        <div class="form-card">
            <h4>😴 ${editingSleepId ? '编辑睡眠' : '记录睡眠'}</h4>
            ${editingSleepId ? '<div style="background:#eef2ff; padding:8px; border-radius:8px; margin-bottom:12px;">✏️ 正在编辑</div>' : ''}
            
            <div class="form-row">
                <label>📅 日期</label>
                <input type="date" id="sleepDate" value="${currentSleepDate}">
            </div>
            
            <div class="time-row">
                <div class="form-row">
                    <label>🛏️ 几点睡？</label>
                    <input type="time" id="bedtime" placeholder="例如: 23:00">
                </div>
                <div class="form-row">
                    <label>⏰ 几点起？</label>
                    <input type="time" id="wakeupTime" placeholder="例如: 07:00">
                </div>
            </div>
            
            <div class="form-row">
                <label>⏱️ 睡眠时长</label>
                <input type="text" id="sleepDurationDisplay" readonly placeholder="自动计算" style="background:#f5f5f5;">
            </div>
            
            <div class="form-row">
                <label>💤 今晚睡得怎么样？</label>
                <div style="display: flex; gap: 12px; margin-top: 8px;">
                    <label style="display: flex; flex-direction: column; align-items: center; gap: 4px; cursor: pointer; padding: 8px; border-radius: 12px; background: #f0f2f5; flex: 1;">
                        <input type="radio" name="sleepQuality" value="EXCELLENT" style="margin: 0;">
                        <span style="font-size: 1.5rem;">🌟</span>
                        <span style="font-size: 0.7rem;">超好</span>
                    </label>
                    <label style="display: flex; flex-direction: column; align-items: center; gap: 4px; cursor: pointer; padding: 8px; border-radius: 12px; background: #f0f2f5; flex: 1;">
                        <input type="radio" name="sleepQuality" value="GOOD" style="margin: 0;">
                        <span style="font-size: 1.5rem;">😊</span>
                        <span style="font-size: 0.7rem;">不错</span>
                    </label>
                    <label style="display: flex; flex-direction: column; align-items: center; gap: 4px; cursor: pointer; padding: 8px; border-radius: 12px; background: #f0f2f5; flex: 1;">
                        <input type="radio" name="sleepQuality" value="FAIR" style="margin: 0;">
                        <span style="font-size: 1.5rem;">😐</span>
                        <span style="font-size: 0.7rem;">一般</span>
                    </label>
                    <label style="display: flex; flex-direction: column; align-items: center; gap: 4px; cursor: pointer; padding: 8px; border-radius: 12px; background: #f0f2f5; flex: 1;">
                        <input type="radio" name="sleepQuality" value="POOR" style="margin: 0;">
                        <span style="font-size: 1.5rem;">😔</span>
                        <span style="font-size: 0.7rem;">较差</span>
                    </label>
                </div>
            </div>
            
            <div class="form-row">
                <label>📝 备注（选填）</label>
                <textarea id="sleepNotes" rows="2" placeholder="比如：喝了咖啡、做了噩梦、被吵醒..." style="width:100%; padding:10px; border-radius:10px; border:1px solid #e0e4e8; resize:none;"></textarea>
            </div>
            
            <div style="display: flex; gap: 10px;">
                <button class="btn-primary" onclick="submitSleepRecord()">💤 ${editingSleepId ? '保存' : '记录'}</button>
                ${editingSleepId ? '<button class="btn-secondary" onclick="cancelSleepEdit()">取消</button>' : ''}
            </div>
        </div>
        
        <div class="form-card">
            <h4>📊 今日睡眠</h4>
            <div id="todaySleepRecords" class="record-list"></div>
        </div>
        
        <div class="sleep-report-card">
            <div class="sleep-report-header">
                <h4>📈 近7日睡眠报告</h4>
                <button class="refresh-report-btn" onclick="loadWeekSleepReport()">🔄 刷新</button>
            </div>
            <div id="weekSleepReport" class="sleep-week-report">
                <div class="loading-spinner"></div> 加载中...
            </div>
        </div>
    `;

    // 绑定时间变化自动计算时长
    const bedtimeInput = document.getElementById('bedtime');
    const wakeupInput = document.getElementById('wakeupTime');
    if (bedtimeInput && wakeupInput) {
        bedtimeInput.onchange = calculateSleepDuration;
        wakeupInput.onchange = calculateSleepDuration;
    }

    loadTodaySleepRecords();
    loadWeekSleepReport();
}

// 自动计算睡眠时长
function calculateSleepDuration() {
    const bedtime = document.getElementById('bedtime').value;
    const wakeupTime = document.getElementById('wakeupTime').value;
    const durationDisplay = document.getElementById('sleepDurationDisplay');

    if (!bedtime || !wakeupTime) {
        if (durationDisplay) durationDisplay.value = '';
        return;
    }

    const [bedHour, bedMinute] = bedtime.split(':').map(Number);
    const [wakeHour, wakeMinute] = wakeupTime.split(':').map(Number);

    let bedDate = new Date();
    bedDate.setHours(bedHour, bedMinute, 0);

    let wakeDate = new Date();
    wakeDate.setHours(wakeHour, wakeMinute, 0);

    if (wakeDate <= bedDate) {
        wakeDate.setDate(wakeDate.getDate() + 1);
    }

    const durationHours = (wakeDate - bedDate) / (1000 * 60 * 60);

    if (durationDisplay) {
        durationDisplay.value = durationHours.toFixed(1) + ' 小时';
    }
}

// 获取选中的睡眠质量
function getSelectedQuality() {
    const radios = document.querySelectorAll('input[name="sleepQuality"]');
    for (const radio of radios) {
        if (radio.checked) return radio.value;
    }
    return 'FAIR';
}

// 提交睡眠记录
async function submitSleepRecord() {
    if (!currentUser) {
        alert('请先登录');
        return;
    }

    const bedtime = document.getElementById('bedtime').value;
    const wakeupTime = document.getElementById('wakeupTime').value;
    const quality = getSelectedQuality();
    const notes = document.getElementById('sleepNotes').value;

    if (!bedtime || !wakeupTime) {
        alert('请填写睡觉和起床时间');
        return;
    }

    // 计算睡眠时长
    const [bedHour, bedMinute] = bedtime.split(':').map(Number);
    const [wakeHour, wakeMinute] = wakeupTime.split(':').map(Number);
    let bedDate = new Date();
    bedDate.setHours(bedHour, bedMinute, 0);
    let wakeDate = new Date();
    wakeDate.setHours(wakeHour, wakeMinute, 0);
    if (wakeDate <= bedDate) wakeDate.setDate(wakeDate.getDate() + 1);
    const sleepDuration = (wakeDate - bedDate) / (1000 * 60 * 60);

    const record = {
        userId: currentUser.userId,
        recordDate: document.getElementById('sleepDate').value || currentSleepDate,
        bedtime: bedtime + ':00',
        wakeupTime: wakeupTime + ':00',
        quality: quality,
        sleepDuration: sleepDuration.toFixed(1),
        notes: notes || null
    };

    const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';

    try {
        let response;
        if (editingSleepId) {
            record.id = editingSleepId;
            response = await fetch(`${baseUrl}/api/sleep/record/${editingSleepId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(record)
            });
        } else {
            response = await fetch(`${baseUrl}/api/sleep/record`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(record)
            });
        }
        const data = await response.json();
        if (data.success) {
            alert(editingSleepId ? `修改成功！评分：${data.sleepScore}/100` : `记录成功！评分：${data.sleepScore}/100`);
            cancelSleepEdit();
            await loadTodaySleepRecords();
            await loadWeekSleepReport();
        } else {
            alert('操作失败: ' + (data.message || '未知错误'));
        }
    } catch (error) {
        console.error('操作失败:', error);
        alert('网络错误: ' + error.message);
    }
}

// 取消编辑
function cancelSleepEdit() {
    editingSleepId = null;
    document.getElementById('bedtime').value = '';
    document.getElementById('wakeupTime').value = '';
    document.getElementById('sleepDurationDisplay').value = '';
    document.getElementById('sleepNotes').value = '';
    const radios = document.querySelectorAll('input[name="sleepQuality"]');
    radios.forEach(r => r.checked = false);
    renderSleepPanel();
}

// 编辑睡眠记录
async function editSleepRecord(recordId) {
    if (!currentUser) return;
    const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';
    try {
        const response = await fetch(`${baseUrl}/api/sleep/week/${currentUser.userId}`);
        const data = await response.json();
        const record = data.records.find(r => r.id === recordId);
        if (record) {
            editingSleepId = recordId;
            document.getElementById('sleepDate').value = record.recordDate;
            document.getElementById('bedtime').value = record.bedtime ? record.bedtime.substring(0, 5) : '';
            document.getElementById('wakeupTime').value = record.wakeupTime ? record.wakeupTime.substring(0, 5) : '';
            document.getElementById('sleepNotes').value = record.notes || '';
            const radios = document.querySelectorAll('input[name="sleepQuality"]');
            radios.forEach(r => { if (r.value === record.quality) r.checked = true; });
            calculateSleepDuration();
            renderSleepPanel();
        } else {
            alert('未找到记录');
        }
    } catch (error) {
        console.error('获取记录失败:', error);
        alert('获取记录失败');
    }
}

// 删除睡眠记录
async function deleteSleepRecord(recordId) {
    if (!currentUser) return;
    if (!confirm('确定删除这条记录吗？')) return;
    const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';
    try {
        const response = await fetch(`${baseUrl}/api/sleep/record/${recordId}?userId=${currentUser.userId}`, {
            method: 'DELETE'
        });
        const data = await response.json();
        if (data.success) {
            alert('删除成功');
            await loadTodaySleepRecords();
            await loadWeekSleepReport();
        } else {
            alert('删除失败');
        }
    } catch (error) {
        console.error('删除失败:', error);
        alert('网络错误');
    }
}

// 加载今日睡眠记录
async function loadTodaySleepRecords() {
    if (!currentUser) return;
    const container = document.getElementById('todaySleepRecords');
    if (!container) return;
    const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';
    try {
        const response = await fetch(`${baseUrl}/api/sleep/today/${currentUser.userId}`);
        const data = await response.json();
        if (data.records && data.records.length > 0) {
            container.innerHTML = data.records.map(record => {
                let qualityText = { 'EXCELLENT':'优秀','GOOD':'良好','FAIR':'一般','POOR':'较差' }[record.quality] || '未知';
                let sleepScore = record.sleepDuration >= 7 && record.sleepDuration <= 9 ? '✅' : (record.sleepDuration >= 6 ? '⚠️' : '❌');
                return `<div class="today-sleep-item">
                    <div class="today-sleep-info">
                        <strong>📅 ${record.recordDate}</strong>
                        <div class="today-sleep-time">🕐 ${record.bedtime?.substring(0,5)} → ${record.wakeupTime?.substring(0,5)} | ⏱️ ${record.sleepDuration}小时 | 💤 ${qualityText}</div>
                        ${record.notes ? '<div>📝 ' + escapeHtml(record.notes) + '</div>' : ''}
                    </div>
                    <div class="today-sleep-actions">
                        <button class="edit-sleep-btn" onclick="editSleepRecord('${record.id}')">✏️</button>
                        <button class="delete-sleep-btn" onclick="deleteSleepRecord('${record.id}')">🗑️</button>
                    </div>
                </div>`;
            }).join('');
        } else {
            container.innerHTML = '<div class="empty-state">😴 今天还没记录睡眠</div>';
        }
    } catch (error) {
        container.innerHTML = '<p style="color:#999;">加载失败</p>';
    }
}

// 加载近7日睡眠报告（简化版）
async function loadWeekSleepReport() {
    if (!currentUser) return;
    const container = document.getElementById('weekSleepReport');
    if (!container) return;
    container.innerHTML = '<div class="loading-spinner"></div>';
    const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';
    try {
        const response = await fetch(`${baseUrl}/api/sleep/week/${currentUser.userId}`);
        const data = await response.json();
        if (data.records && data.records.length > 0) {
            container.innerHTML = generateWeekSleepReportHtml(data.records);
        } else {
            container.innerHTML = '<div class="empty-state">😴 暂无睡眠记录</div>';
        }
    } catch (error) {
        container.innerHTML = '<p style="color:#999;">加载失败</p>';
    }
}

// 生成周报告HTML（简化版，增加饮食-睡眠关联提示）
function generateWeekSleepReportHtml(records) {
    const groupedByDate = {};
    for (const record of records) {
        const date = record.recordDate;
        if (!groupedByDate[date]) groupedByDate[date] = [];
        groupedByDate[date].push(record);
    }

    let totalDuration = 0, totalScore = 0;
    for (const record of records) {
        totalDuration += record.sleepDuration || 0;
        totalScore += calculateSleepScoreFromRecord(record);
    }
    const avgDuration = (totalDuration / records.length).toFixed(1);
    const avgScore = (totalScore / records.length).toFixed(0);

    let scoreLevel = avgScore >= 80 ? '优秀' : (avgScore >= 60 ? '良好' : '需改善');
    let scoreColor = avgScore >= 80 ? '#10b981' : (avgScore >= 60 ? '#f59e0b' : '#ef4444');

    let html = `
        <div style="padding: 8px;">
            <div style="text-align:center; margin-bottom:20px;">
                <h3>📊 近7日睡眠报告</h3>
                <p style="color:#666;">${new Date().toLocaleDateString()}</p>
            </div>
            <div class="stats-cards">
                <div class="stat-card purple"><div class="stat-value">${avgScore}</div><div class="stat-label">平均评分</div><div style="font-size:0.7rem;">${scoreLevel}</div></div>
                <div class="stat-card blue"><div class="stat-value">${avgDuration}</div><div class="stat-label">平均时长(小时)</div></div>
                <div class="stat-card green"><div class="stat-value">${records.length}</div><div class="stat-label">记录天数</div></div>
            </div>
    `;

    // 睡眠建议
    let advice = '';
    if (avgDuration < 7) advice = '⏰ 睡眠不足，建议23点前入睡';
    else if (avgDuration > 9) advice = '😴 睡得有点多哦';
    else advice = '✅ 睡眠时长达标，继续保持！';
    html += `<div style="background:#eef2ff; padding:12px; border-radius:12px; margin-bottom:16px;">💡 ${advice}</div>`;

    html += `<h4>📋 每日详情</h4>`;
    const sortedDates = Object.keys(groupedByDate).sort().reverse();
    for (const date of sortedDates) {
        const dayRecords = groupedByDate[date];
        html += `<div style="margin-bottom:16px; border-left:3px solid #667eea; padding-left:12px;">
            <div style="display:flex; justify-content:space-between;"><strong>📅 ${date}</strong></div>`;
        for (const record of dayRecords) {
            let qualityIcon = { 'EXCELLENT':'🌟','GOOD':'😊','FAIR':'😐','POOR':'😔' }[record.quality] || '😐';
            html += `<div style="font-size:0.85rem; padding:4px 0;">${qualityIcon} ${record.bedtime?.substring(0,5)} → ${record.wakeupTime?.substring(0,5)} | ${record.sleepDuration}小时</div>`;
            if (record.notes) html += `<div style="font-size:0.75rem; color:#666; padding-left:20px;">📝 ${escapeHtml(record.notes)}</div>`;
        }
        html += `</div>`;
    }
    html += `</div>`;
    return html;
}

function calculateSleepScoreFromRecord(record) {
    let score = 70;
    if (record.sleepDuration) {
        if (record.sleepDuration >= 7 && record.sleepDuration <= 9) score += 15;
        else if (record.sleepDuration >= 6) score += 5;
        else score -= 10;
    }
    if (record.quality) {
        if (record.quality === 'EXCELLENT') score += 20;
        else if (record.quality === 'GOOD') score += 10;
        else if (record.quality === 'POOR') score -= 15;
    }
    if (record.bedtime) {
        const hour = parseInt(record.bedtime.substring(0,2));
        if (hour >= 21 && hour <= 23) score += 10;
        else if (hour >= 23 || hour <= 1) score += 5;
        else score -= 10;
    }
    return Math.max(0, Math.min(100, score));
}

// 挂载全局函数
window.renderSleepPanel = renderSleepPanel;
window.submitSleepRecord = submitSleepRecord;
window.editSleepRecord = editSleepRecord;
window.deleteSleepRecord = deleteSleepRecord;
window.cancelSleepEdit = cancelSleepEdit;
window.loadTodaySleepRecords = loadTodaySleepRecords;
window.loadWeekSleepReport = loadWeekSleepReport;