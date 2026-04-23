// sleep.js - 睡眠功能模块（完整版）

let currentSleepDate = new Date().toISOString().split('T')[0];
let editingSleepId = null;  // 正在编辑的记录ID

// 渲染睡眠面板
function renderSleepPanel() {
    const panel = document.getElementById('sleep-panel');
    if (!panel) return;

    panel.innerHTML = `
        <div class="form-card">
            <h4>😴 ${editingSleepId ? '编辑睡眠记录' : '记录今日睡眠'}</h4>
            ${editingSleepId ? '<div style="background:#eef2ff; padding:8px; border-radius:8px; margin-bottom:12px;">✏️ 正在编辑模式</div>' : ''}
            <div class="form-row">
                <label>记录日期</label>
                <input type="date" id="sleepDate" value="${currentSleepDate}">
            </div>
            <div class="time-row">
                <div class="form-row">
                    <label>🛏️ 就寝时间</label>
                    <input type="time" id="bedtime" placeholder="例如: 22:30">
                </div>
                <div class="form-row">
                    <label>⏰ 起床时间</label>
                    <input type="time" id="wakeupTime" placeholder="例如: 07:00">
                </div>
            </div>
            <div class="form-row">
                <label>⏱️ 睡眠时长</label>
                <input type="text" id="sleepDurationDisplay" readonly placeholder="自动计算" style="background:#f5f5f5;">
            </div>
            <div class="form-row">
                <label>💤 睡眠质量</label>
                <select id="sleepQuality">
                    <option value="">请选择</option>
                    <option value="EXCELLENT">🌟 优秀</option>
                    <option value="GOOD">😊 良好</option>
                    <option value="FAIR">😐 一般</option>
                    <option value="POOR">😔 较差</option>
                </select>
            </div>
            <div class="form-row">
                <label>😴 深睡时长 (分钟)</label>
                <input type="number" id="deepSleepMinutes" placeholder="例如: 120">
            </div>
            <div class="form-row">
                <label>🔄 醒来次数</label>
                <input type="number" id="wakeCount" placeholder="例如: 1">
            </div>
            <div class="form-row">
                <label>📝 备注</label>
                <textarea id="sleepNotes" rows="2" placeholder="睡眠感受、梦境等（可选）" style="width:100%; padding:10px; border-radius:10px; border:1px solid #e0e4e8; resize:none;"></textarea>
            </div>
            <div style="display: flex; gap: 10px;">
                <button class="btn-primary" onclick="submitSleepRecord()">💤 ${editingSleepId ? '保存修改' : '记录睡眠'}</button>
                ${editingSleepId ? '<button class="btn-secondary" onclick="cancelSleepEdit()">取消编辑</button>' : ''}
            </div>
        </div>
        
        <div class="form-card">
            <h4>📊 今日睡眠</h4>
            <div id="todaySleepRecords" class="record-list"></div>
        </div>
        
        <div class="sleep-report-card">
            <div class="sleep-report-header">
                <h4>📈 近7日睡眠报告</h4>
                <button class="refresh-report-btn" onclick="loadWeekSleepReport()">🔄 刷新报告</button>
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
    const durationHidden = document.getElementById('sleepDuration');

    if (!bedtime || !wakeupTime) {
        if (durationDisplay) durationDisplay.value = '';
        if (durationHidden) durationHidden.value = '';
        return;
    }

    // 解析时间
    const [bedHour, bedMinute] = bedtime.split(':').map(Number);
    const [wakeHour, wakeMinute] = wakeupTime.split(':').map(Number);

    let bedDate = new Date();
    bedDate.setHours(bedHour, bedMinute, 0);

    let wakeDate = new Date();
    wakeDate.setHours(wakeHour, wakeMinute, 0);

    // 如果起床时间早于就寝时间，说明跨天了
    if (wakeDate <= bedDate) {
        wakeDate.setDate(wakeDate.getDate() + 1);
    }

    const durationMs = wakeDate - bedDate;
    const durationHours = durationMs / (1000 * 60 * 60);

    if (durationDisplay) {
        durationDisplay.value = durationHours.toFixed(1) + ' 小时';
    }

    // 同时设置隐藏的数值字段
    if (!document.getElementById('sleepDuration')) {
        const hidden = document.createElement('input');
        hidden.type = 'hidden';
        hidden.id = 'sleepDuration';
        document.querySelector('.form-card').appendChild(hidden);
    }
    document.getElementById('sleepDuration').value = durationHours.toFixed(1);
}

// 提交睡眠记录（新增或编辑）
async function submitSleepRecord() {
    if (!currentUser) {
        alert('请先登录');
        return;
    }

    const bedtime = document.getElementById('bedtime').value;
    const wakeupTime = document.getElementById('wakeupTime').value;
    const sleepDuration = parseFloat(document.getElementById('sleepDuration')?.value);
    const deepSleepMinutes = parseInt(document.getElementById('deepSleepMinutes').value);
    const wakeCount = parseInt(document.getElementById('wakeCount').value);
    const quality = document.getElementById('sleepQuality').value;
    const notes = document.getElementById('sleepNotes').value;

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

    const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';

    try {
        let response;
        let url;

        if (editingSleepId) {
            // 编辑模式：使用 PUT 请求
            url = `${baseUrl}/api/sleep/record/${editingSleepId}`;
            record.id = editingSleepId;
            response = await fetch(url, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(record)
            });
        } else {
            // 新增模式
            url = `${baseUrl}/api/sleep/record`;
            response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(record)
            });
        }

        const data = await response.json();
        if (data.success) {
            alert(editingSleepId ? `修改成功！睡眠评分：${data.sleepScore}/100` : `记录成功！睡眠评分：${data.sleepScore}/100`);
            // 清空表单
            cancelSleepEdit();
            // 刷新数据
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
    document.getElementById('sleepQuality').value = '';
    document.getElementById('sleepDurationDisplay').value = '';
    document.getElementById('deepSleepMinutes').value = '';
    document.getElementById('wakeCount').value = '';
    document.getElementById('sleepNotes').value = '';
    renderSleepPanel();
}

// 编辑睡眠记录
async function editSleepRecord(recordId) {
    if (!currentUser) return;

    const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';

    try {
        // 从今日记录或周记录中获取数据
        const response = await fetch(`${baseUrl}/api/sleep/week/${currentUser.userId}`);
        const data = await response.json();
        const record = data.records.find(r => r.id === recordId);

        if (record) {
            editingSleepId = recordId;
            document.getElementById('sleepDate').value = record.recordDate;
            document.getElementById('bedtime').value = record.bedtime ? record.bedtime.substring(0, 5) : '';
            document.getElementById('wakeupTime').value = record.wakeupTime ? record.wakeupTime.substring(0, 5) : '';
            document.getElementById('sleepQuality').value = record.quality || '';
            document.getElementById('deepSleepMinutes').value = record.deepSleepMinutes || '';
            document.getElementById('wakeCount').value = record.wakeCount || '';
            document.getElementById('sleepNotes').value = record.notes || '';
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
    if (!confirm('确定要删除这条睡眠记录吗？')) return;

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
            alert('删除失败: ' + (data.message || '未知错误'));
        }
    } catch (error) {
        console.error('删除失败:', error);
        alert('网络错误: ' + error.message);
    }
}

// 加载今日睡眠记录（带编辑删除按钮）
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
                    case 'EXCELLENT': qualityText = '优秀'; qualityClass = 'sleep-quality-excellent'; break;
                    case 'GOOD': qualityText = '良好'; qualityClass = 'sleep-quality-good'; break;
                    case 'FAIR': qualityText = '一般'; qualityClass = 'sleep-quality-fair'; break;
                    case 'POOR': qualityText = '较差'; qualityClass = 'sleep-quality-poor'; break;
                }

                let sleepScoreClass = 'sleep-score-fair';
                let sleepScoreText = '--';
                if (record.sleepDuration) {
                    if (record.sleepDuration >= 7 && record.sleepDuration <= 9) {
                        sleepScoreText = '优秀';
                        sleepScoreClass = 'sleep-score-excellent';
                    } else if (record.sleepDuration >= 6) {
                        sleepScoreText = '良好';
                        sleepScoreClass = 'sleep-score-good';
                    } else {
                        sleepScoreText = '不足';
                        sleepScoreClass = 'sleep-score-poor';
                    }
                }

                return `<div class="today-sleep-item">
                    <div class="today-sleep-info">
                        <strong>📅 ${record.recordDate}</strong>
                        <div class="today-sleep-time">
                            🕐 ${record.bedtime?.substring(0,5) || '?'} → ${record.wakeupTime?.substring(0,5) || '?'}
                            <span class="${sleepScoreClass}" style="margin-left: 8px;">${sleepScoreText}</span>
                        </div>
                        <div>⏱️ ${record.sleepDuration || '?'}小时 | 💤 ${qualityText}</div>
                        ${record.notes ? '<div>📝 ' + escapeHtml(record.notes) + '</div>' : ''}
                    </div>
                    <div class="today-sleep-actions">
                        <button class="edit-sleep-btn" onclick="editSleepRecord('${record.id}')" title="编辑">✏️</button>
                        <button class="delete-sleep-btn" onclick="deleteSleepRecord('${record.id}')" title="删除">🗑️</button>
                    </div>
                </div>`;
            }).join('');
        } else {
            container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">😴</div><p>今日暂无睡眠记录</p></div>';
        }
    } catch (error) {
        console.error('加载睡眠记录失败:', error);
        container.innerHTML = '<p style="color:#999; text-align:center;">加载失败</p>';
    }
}

// 加载近7日睡眠报告
async function loadWeekSleepReport() {
    if (!currentUser) return;
    const container = document.getElementById('weekSleepReport');
    if (!container) return;

    container.innerHTML = '<div class="loading-spinner"></div> 加载中...';

    try {
        const baseUrl = typeof API_BASE !== 'undefined' ? API_BASE : '';
        const response = await fetch(`${baseUrl}/api/sleep/week/${currentUser.userId}`);
        const data = await response.json();

        if (data.records && data.records.length > 0) {
            const reportHtml = generateWeekSleepReportHtml(data.records);
            container.innerHTML = reportHtml;
        } else {
            container.innerHTML = '<div class="empty-state"><div class="empty-state-icon">😴</div><p>暂无睡眠记录，请先记录您的睡眠</p></div>';
        }
    } catch (error) {
        console.error('加载周报告失败:', error);
        container.innerHTML = '<p style="color:#999; text-align:center;">加载失败，请检查网络连接</p>';
    }
}

// 生成近7日睡眠报告HTML
function generateWeekSleepReportHtml(records) {
    // 按日期分组
    const groupedByDate = {};
    for (const record of records) {
        const date = record.recordDate;
        if (!groupedByDate[date]) {
            groupedByDate[date] = [];
        }
        groupedByDate[date].push(record);
    }

    // 计算统计数据
    let totalDuration = 0;
    let totalScore = 0;
    let totalDeepSleep = 0;
    let totalWakeCount = 0;
    let excellentCount = 0;
    let goodCount = 0;

    for (const record of records) {
        totalDuration += record.sleepDuration || 0;
        const score = calculateSleepScoreFromRecord(record);
        totalScore += score;
        totalDeepSleep += record.deepSleepMinutes || 0;
        totalWakeCount += record.wakeCount || 0;
        if (score >= 80) excellentCount++;
        else if (score >= 60) goodCount++;
    }

    const avgDuration = (totalDuration / records.length).toFixed(1);
    const avgScore = (totalScore / records.length).toFixed(0);
    const avgDeepSleep = (totalDeepSleep / records.length).toFixed(0);
    const avgWakeCount = (totalWakeCount / records.length).toFixed(1);

    let scoreLevel = '';
    let scoreColor = '';
    if (avgScore >= 80) {
        scoreLevel = '优秀';
        scoreColor = '#10b981';
    } else if (avgScore >= 60) {
        scoreLevel = '良好';
        scoreColor = '#f59e0b';
    } else {
        scoreLevel = '需改进';
        scoreColor = '#ef4444';
    }

    let html = `
        <div style="padding: 8px;">
            <div style="text-align: center; margin-bottom: 20px;">
                <span style="font-size: 2rem;">📊</span>
                <h3 style="margin: 8px 0 4px;">近7日睡眠报告</h3>
                <p style="color: #666; font-size: 0.8rem;">${new Date().toLocaleDateString()}</p>
            </div>
            
            <div class="stats-cards">
                <div class="stat-card purple">
                    <div class="stat-value">${avgScore}</div>
                    <div class="stat-label">平均睡眠评分</div>
                    <div style="font-size:0.7rem; margin-top:4px;">${scoreLevel}</div>
                </div>
                <div class="stat-card blue">
                    <div class="stat-value">${avgDuration}</div>
                    <div class="stat-label">平均时长(小时)</div>
                </div>
                <div class="stat-card green">
                    <div class="stat-value">${avgDeepSleep}</div>
                    <div class="stat-label">平均深睡(分钟)</div>
                </div>
                <div class="stat-card orange">
                    <div class="stat-value">${avgWakeCount}</div>
                    <div class="stat-label">平均醒来次数</div>
                </div>
            </div>
            
            <div style="margin-bottom: 20px;">
                <h4 style="margin-bottom: 12px;">📈 睡眠质量分布</h4>
                <div style="display: flex; gap: 12px; flex-wrap: wrap;">
                    <div style="flex:1; background: #d1fae5; padding: 12px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.5rem; font-weight: bold; color: #065f46;">${excellentCount}</div>
                        <div style="font-size: 0.7rem; color: #065f46;">优质睡眠(≥80分)</div>
                    </div>
                    <div style="flex:1; background: #dbeafe; padding: 12px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.5rem; font-weight: bold; color: #1e40af;">${goodCount}</div>
                        <div style="font-size: 0.7rem; color: #1e40af;">良好睡眠(60-79分)</div>
                    </div>
                    <div style="flex:1; background: #fee2e2; padding: 12px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.5rem; font-weight: bold; color: #991b1b;">${records.length - excellentCount - goodCount}</div>
                        <div style="font-size: 0.7rem; color: #991b1b;">需改善(&lt;60分)</div>
                    </div>
                </div>
            </div>
            
            <h4 style="margin-bottom: 12px;">📋 每日详情</h4>
    `;

    const sortedDates = Object.keys(groupedByDate).sort().reverse();
    for (const date of sortedDates) {
        const dayRecords = groupedByDate[date];
        const dayAvgScore = (dayRecords.reduce((sum, r) => sum + calculateSleepScoreFromRecord(r), 0) / dayRecords.length).toFixed(0);

        let qualityClass = 'sleep-quality-fair';
        if (dayAvgScore >= 80) qualityClass = 'sleep-quality-excellent';
        else if (dayAvgScore >= 60) qualityClass = 'sleep-quality-good';
        else if (dayAvgScore >= 40) qualityClass = 'sleep-quality-fair';
        else qualityClass = 'sleep-quality-poor';

        let qualityText = '一般';
        if (dayAvgScore >= 80) qualityText = '优秀';
        else if (dayAvgScore >= 60) qualityText = '良好';
        else if (dayAvgScore >= 40) qualityText = '一般';
        else qualityText = '较差';

        html += `
            <div class="date-group">
                <div class="date-header">
                    <strong class="date-title">📅 ${date}</strong>
                    <span class="sleep-quality-badge ${qualityClass}">${qualityText} ${dayAvgScore}分</span>
                </div>
        `;

        for (const record of dayRecords) {
            const score = calculateSleepScoreFromRecord(record);
            let scoreClass = 'sleep-score-fair';
            if (score >= 80) scoreClass = 'sleep-score-excellent';
            else if (score >= 60) scoreClass = 'sleep-score-good';
            else if (score >= 40) scoreClass = 'sleep-score-fair';
            else scoreClass = 'sleep-score-poor';

            let qualityIcon = '😐';
            if (record.quality === 'EXCELLENT') qualityIcon = '🌟';
            else if (record.quality === 'GOOD') qualityIcon = '😊';
            else if (record.quality === 'FAIR') qualityIcon = '😐';
            else if (record.quality === 'POOR') qualityIcon = '😔';

            html += `
                <div class="sleep-record-item">
                    <div class="sleep-record-info">
                        <span>${qualityIcon} ${record.bedtime?.substring(0,5) || '?'} → ${record.wakeupTime?.substring(0,5) || '?'}</span>
                        <span>⏱️ ${record.sleepDuration || '?'}小时</span>
                        <span>💤 ${record.deepSleepMinutes || 0}分钟深睡</span>
                        <span>🔄 醒来${record.wakeCount || 0}次</span>
                    </div>
                    <div class="sleep-record-score ${scoreClass}">${score}分</div>
                </div>
            `;
        }

        html += `</div>`;
    }

    // 睡眠建议
    html += `
            <div style="margin-top: 20px; padding: 12px; background: #eef2ff; border-radius: 8px;">
                <div style="font-size: 0.8rem; color: #667eea;">💡 睡眠建议</div>
                <div style="font-size: 0.8rem; margin-top: 8px;">
                    ${generateSleepAdvice(avgDuration, avgScore, avgDeepSleep, avgWakeCount)}
                </div>
            </div>
        </div>
    `;

    return html;
}

// 计算睡眠评分（从记录对象）
function calculateSleepScoreFromRecord(record) {
    let score = 70;

    if (record.sleepDuration) {
        if (record.sleepDuration >= 7 && record.sleepDuration <= 9) score += 15;
        else if (record.sleepDuration >= 6 || record.sleepDuration <= 10) score += 5;
        else score -= 10;
    }

    if (record.quality) {
        switch (record.quality) {
            case 'EXCELLENT': score += 20; break;
            case 'GOOD': score += 10; break;
            case 'POOR': score -= 15; break;
        }
    }

    if (record.bedtime) {
        const hour = parseInt(record.bedtime.substring(0, 2));
        if (hour >= 21 && hour <= 23) score += 10;
        else if (hour >= 23 || hour <= 1) score += 5;
        else score -= 10;
    }

    return Math.max(0, Math.min(100, score));
}

// 生成睡眠建议
function generateSleepAdvice(avgDuration, avgScore, avgDeepSleep, avgWakeCount) {
    const advices = [];

    if (avgScore >= 80) {
        advices.push('🎉 睡眠质量优秀！继续保持良好的作息习惯！');
    } else if (avgScore >= 60) {
        advices.push('📌 睡眠质量良好，仍有提升空间。');
    } else {
        advices.push('⚠️ 睡眠质量需要改善，请关注以下建议。');
    }

    if (avgDuration < 7) {
        advices.push('⏰ 睡眠时长不足（平均' + avgDuration + '小时），建议保证7-8小时睡眠。');
    } else if (avgDuration > 9) {
        advices.push('😴 睡眠时间偏长，注意不要睡过头哦。');
    }

    if (avgDeepSleep < 60) {
        advices.push('💤 深睡时长偏短，建议睡前放松，避免使用电子设备。');
    }

    if (avgWakeCount > 2) {
        advices.push('🔄 夜间醒来次数较多，建议检查睡眠环境（温度、光线、噪音）。');
    }

    if (advices.length === 0) {
        advices.push('👍 睡眠状况良好，继续保持！');
    }

    return advices.join('<br>');
}

// 加载原有报告（保留兼容）
async function loadSleepReport() {
    await loadWeekSleepReport();
}

// 挂载全局函数
window.renderSleepPanel = renderSleepPanel;
window.addSleepRecord = submitSleepRecord;
window.submitSleepRecord = submitSleepRecord;
window.editSleepRecord = editSleepRecord;
window.deleteSleepRecord = deleteSleepRecord;
window.cancelSleepEdit = cancelSleepEdit;
window.loadTodaySleepRecords = loadTodaySleepRecords;
window.loadSleepReport = loadSleepReport;
window.loadWeekSleepReport = loadWeekSleepReport;