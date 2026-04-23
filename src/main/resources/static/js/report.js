// report.js - 七日健康报告
// 在 report.js 顶部添加
function showToast(message, type) {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%);
        background: #1e293b; color: white; padding: 10px 20px;
        border-radius: 30px; font-size: 0.85rem; z-index: 1000;
        animation: fadeInUp 0.3s ease;
    `;
    toast.innerText = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2000);
}

function renderReportPanel() {
    const panel = document.getElementById('report-panel');
    if (!panel) return;

    panel.innerHTML = `
        <div class="form-card" id="reportContent">
            <div class="loading-spinner"></div> 加载中...
        </div>
        <div style="display: flex; gap: 10px; margin-top: 12px;">
            <button class="btn-primary" onclick="refreshReport()">🔄 刷新报告</button>
        </div>
    `;
}

async function loadSevenDayReport() {
    if (!currentUser) return;

    const container = document.getElementById('reportContent');
    if (!container) return;

    try {
        // 并行获取饮食和睡眠数据
        const [dietRes, sleepRes] = await Promise.all([
            fetch(`${API_BASE}/api/diet/week/${currentUser.userId}`),
            fetch(`${API_BASE}/api/sleep/week/${currentUser.userId}`)
        ]);

        const dietData = await dietRes.json();
        const sleepData = await sleepRes.json();

        const dietRecords = dietData.records || [];
        const sleepRecords = sleepData.records || [];

        // 生成报告
        const reportHtml = generateSevenDayReport(dietRecords, sleepRecords);
        container.innerHTML = reportHtml;

    } catch (error) {
        console.error('加载报告失败:', error);
        container.innerHTML = '<div class="empty-state">⚠️ 加载失败，请稍后重试</div>';
    }
}

function generateSevenDayReport(dietRecords, sleepRecords) {
    // 计算饮食评分
    let dietTotalScore = 0;
    let breakfastCount = 0, lunchCount = 0, dinnerCount = 0;
    let vegetableCount = 0, proteinCount = 0;
    let totalVegetables = 0, totalFruit = 0, totalProtein = 0;
    let lateDinnerCount = 0;
    const uniqueFoods = new Set();
    const dietDates = new Set();

    for (const record of dietRecords) {
        dietTotalScore += record.healthScore || 0;
        uniqueFoods.add(record.foodName);

        if (record.recordDate) {
            const date = record.recordDate.split('T')[0];
            dietDates.add(date);
        }

        const foodName = record.foodName || '';
        const grams = record.grams || 0;

        if (record.mealType === 'BREAKFAST') breakfastCount++;
        if (record.mealType === 'LUNCH') lunchCount++;
        if (record.mealType === 'DINNER') {
            dinnerCount++;
            if (record.recordDate) {
                const hour = new Date(record.recordDate).getHours();
                if (hour >= 20) lateDinnerCount++;
            }
        }

        if (foodName.includes('蔬菜') || foodName.includes('青菜') || foodName.includes('菜')) {
            vegetableCount++;
            totalVegetables += grams;
        }
        if (foodName.includes('苹果') || foodName.includes('香蕉') || foodName.includes('橙子') || foodName.includes('猕猴桃')) {
            totalFruit += grams;
        }
        if (foodName.includes('肉') || foodName.includes('蛋') || foodName.includes('鱼') || foodName.includes('豆腐')) {
            proteinCount++;
            totalProtein += grams * 0.2;
        }
    }

    const dietDays = dietDates.size || 1;
    const avgDietScore = dietRecords.length > 0 ? (dietTotalScore / dietRecords.length).toFixed(0) : 0;
    const avgVegetables = (totalVegetables / dietDays).toFixed(0);
    const avgFruit = (totalFruit / dietDays).toFixed(0);
    const avgProtein = (totalProtein / dietDays).toFixed(0);
    const foodTypeCount = uniqueFoods.size;

    // 计算睡眠评分
    let sleepTotalScore = 0;
    let sleepDurationSum = 0;
    let goodSleepCount = 0;
    let before23Count = 0;

    for (const record of sleepRecords) {
        let score = 70;
        if (record.sleepDuration) {
            if (record.sleepDuration >= 7 && record.sleepDuration <= 9) score += 15;
            else if (record.sleepDuration >= 6) score += 5;
            else score -= 10;
            sleepDurationSum += record.sleepDuration;
        }
        if (record.quality) {
            if (record.quality === 'EXCELLENT') { score += 20; goodSleepCount++; }
            else if (record.quality === 'GOOD') { score += 10; goodSleepCount++; }
            else if (record.quality === 'POOR') score -= 15;
        }
        if (record.bedtime) {
            const hour = parseInt(record.bedtime.substring(0,2));
            if (hour >= 21 && hour <= 23) { score += 10; before23Count++; }
            else if (hour >= 23 && hour <= 1) score += 5;
            else score -= 10;
        }
        sleepTotalScore += Math.max(0, Math.min(100, score));
    }

    const avgSleepScore = sleepRecords.length > 0 ? (sleepTotalScore / sleepRecords.length).toFixed(0) : 0;
    const avgSleepDuration = sleepRecords.length > 0 ? (sleepDurationSum / sleepRecords.length).toFixed(1) : 0;
    const totalScore = (parseInt(avgDietScore) + parseInt(avgSleepScore)) / 2;

    // 评分等级
    let totalLevel = '', totalColor = '';
    if (totalScore >= 80) { totalLevel = '优秀'; totalColor = '#10b981'; }
    else if (totalScore >= 60) { totalLevel = '良好'; totalColor = '#f59e0b'; }
    else { totalLevel = '需改进'; totalColor = '#ef4444'; }

    // 生成建议
    const suggestions = [];
    if (avgVegetables < 300) suggestions.push('🥬 蔬菜摄入不足，建议每天吃够300g绿叶蔬菜');
    if (avgFruit < 200) suggestions.push('🍎 水果摄入偏少，每天1-2个水果补充维生素');
    if (avgProtein < 60) suggestions.push('🥩 蛋白质不足，增加鸡蛋、鱼、豆腐');
    if (breakfastCount < 5) suggestions.push('🌅 早餐次数偏少，规律早餐有助于全天代谢');
    if (lateDinnerCount > 3) suggestions.push('🌙 晚餐时间偏晚，建议19:00前完成晚餐');
    if (foodTypeCount < 12) suggestions.push('🍽️ 食物种类偏少，建议每周吃够25种不同食物');
    if (avgSleepDuration < 7) suggestions.push('⏰ 睡眠不足，建议保证7-8小时睡眠');
    if (avgSleepDuration > 9) suggestions.push('😴 睡眠过长，注意不要睡过头');
    if (before23Count < 3 && sleepRecords.length > 0) suggestions.push('🌙 入睡偏晚，23点前睡觉对肝脏最好');

    // 生成关联洞察
    let correlationHtml = '';
    if (lateDinnerCount > 2 && avgSleepScore < 70) {
        correlationHtml = `
            <div style="background: #fef3c7; padding: 12px; border-radius: 12px; margin-bottom: 16px;">
                <div style="color: #d97706; font-weight: bold;">⚠️ 晚餐时间影响睡眠</div>
                <div>本周有 ${lateDinnerCount} 次晚餐在20:00后，睡眠评分偏低。建议晚餐提前至19:00前。</div>
            </div>
        `;
    } else if (vegetableCount > 10 && avgSleepScore > 75) {
        correlationHtml = `
            <div style="background: #d1fae5; padding: 12px; border-radius: 12px; margin-bottom: 16px;">
                <div style="color: #065f46; font-weight: bold;">✅ 饮食-睡眠协同</div>
                <div>蔬菜摄入充足，睡眠质量良好，形成良性循环！</div>
            </div>
        `;
    } else if (avgSleepScore < 65 && avgDietScore < 65) {
        correlationHtml = `
            <div style="background: #fee2e2; padding: 12px; border-radius: 12px; margin-bottom: 16px;">
                <div style="color: #dc2626; font-weight: bold;">⚠️ 饮食-睡眠相互影响</div>
                <div>本周饮食和睡眠质量都偏低，建议从改善晚餐时间开始。</div>
            </div>
        `;
    } else {
        correlationHtml = `
            <div style="background: #e0e7ff; padding: 12px; border-radius: 12px; margin-bottom: 16px;">
                <div style="color: #4338ca; font-weight: bold;">📊 健康状态</div>
                <div>饮食评分${avgDietScore}分，睡眠评分${avgSleepScore}分，${totalLevel}状态。</div>
            </div>
        `;
    }

    const html = `
        <div style="padding: 8px;">
            <!-- 头部 -->
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 24px; border-radius: 20px; margin-bottom: 20px; text-align: center;">
                <div style="font-size: 3rem; margin-bottom: 8px;">📊</div>
                <h2 style="margin: 0; font-size: 1.5rem;">七日健康报告</h2>
                <p style="margin: 8px 0 0; opacity: 0.9;">${new Date().toLocaleDateString()}</p>
            </div>
            
            <!-- 综合评分 -->
            <div style="background: white; border-radius: 16px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                <div style="text-align: center;">
                    <div style="font-size: 0.8rem; color: #666;">综合健康评分</div>
                    <div style="font-size: 3rem; font-weight: bold; color: ${totalColor};">${totalScore.toFixed(0)}</div>
                    <div style="font-size: 0.9rem; color: ${totalColor};">${totalLevel}</div>
                </div>
                <div style="display: flex; justify-content: space-around; margin-top: 20px;">
                    <div style="text-align: center;">
                        <div style="font-size: 0.7rem; color: #666;">饮食评分</div>
                        <div style="font-size: 1.5rem; font-weight: bold; color: ${avgDietScore >= 70 ? '#10b981' : '#f59e0b'};">${avgDietScore}</div>
                        <div style="font-size: 0.7rem;">${avgDietScore >= 80 ? '优秀' : (avgDietScore >= 60 ? '良好' : '需改进')}</div>
                    </div>
                    <div style="text-align: center;">
                        <div style="font-size: 0.7rem; color: #666;">睡眠评分</div>
                        <div style="font-size: 1.5rem; font-weight: bold; color: ${avgSleepScore >= 70 ? '#10b981' : '#f59e0b'};">${avgSleepScore}</div>
                        <div style="font-size: 0.7rem;">${avgSleepScore >= 80 ? '优秀' : (avgSleepScore >= 60 ? '良好' : '需改进')}</div>
                    </div>
                </div>
            </div>
            
            <!-- 饮食详情 -->
            <div style="background: white; border-radius: 16px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                <h3 style="margin: 0 0 12px 0; color: #2c3e50;">🍽️ 本周饮食总结</h3>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px;">
                    <div style="background: #f0f2f5; padding: 10px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.3rem; font-weight: bold;">${dietRecords.length}</div>
                        <div style="font-size: 0.7rem; color: #666;">总记录餐数</div>
                    </div>
                    <div style="background: #f0f2f5; padding: 10px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.3rem; font-weight: bold;">${foodTypeCount}</div>
                        <div style="font-size: 0.7rem; color: #666;">食物种类(周)</div>
                    </div>
                    <div style="background: #f0f2f5; padding: 10px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.3rem; font-weight: bold;">${breakfastCount}</div>
                        <div style="font-size: 0.7rem; color: #666;">早餐次数</div>
                    </div>
                </div>
                <div style="margin-top: 12px;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                        <span>🥬 蔬菜摄入</span>
                        <span>${avgVegetables}g/天</span>
                    </div>
                    <div style="background: #e2e8f0; border-radius: 10px; height: 8px; overflow: hidden;">
                        <div style="width: ${Math.min(100, avgVegetables / 300 * 100)}%; background: #10b981; height: 100%;"></div>
                    </div>
                    <div style="display: flex; justify-content: space-between; margin: 12px 0 8px;">
                        <span>🥩 蛋白质摄入</span>
                        <span>${avgProtein}g/天</span>
                    </div>
                    <div style="background: #e2e8f0; border-radius: 10px; height: 8px; overflow: hidden;">
                        <div style="width: ${Math.min(100, avgProtein / 60 * 100)}%; background: #3b82f6; height: 100%;"></div>
                    </div>
                </div>
            </div>
            
            <!-- 睡眠详情 -->
            <div style="background: white; border-radius: 16px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                <h3 style="margin: 0 0 12px 0; color: #2c3e50;">😴 本周睡眠总结</h3>
                <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px;">
                    <div style="background: #f0f2f5; padding: 10px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.3rem; font-weight: bold;">${sleepRecords.length}</div>
                        <div style="font-size: 0.7rem; color: #666;">记录天数</div>
                    </div>
                    <div style="background: #f0f2f5; padding: 10px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.3rem; font-weight: bold;">${avgSleepDuration}</div>
                        <div style="font-size: 0.7rem; color: #666;">平均时长(小时)</div>
                    </div>
                    <div style="background: #f0f2f5; padding: 10px; border-radius: 12px; text-align: center;">
                        <div style="font-size: 1.3rem; font-weight: bold;">${goodSleepCount}</div>
                        <div style="font-size: 0.7rem; color: #666;">优质睡眠</div>
                    </div>
                </div>
            </div>
            
            <!-- 关联洞察 -->
            <div style="background: white; border-radius: 16px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                <h3 style="margin: 0 0 12px 0; color: #2c3e50;">🔍 关联洞察</h3>
                ${correlationHtml}
            </div>
            
            <!-- 改进建议 -->
            <div style="background: white; border-radius: 16px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                <h3 style="margin: 0 0 12px 0; color: #2c3e50;">💡 个性化建议</h3>
                <ul style="margin: 0; padding-left: 20px;">
                    ${suggestions.map(s => `<li style="margin-bottom: 8px;">${s}</li>`).join('')}
                    ${suggestions.length === 0 ? '<li>👍 各项指标良好，继续保持！</li>' : ''}
                </ul>
            </div>
            
            <!-- 下周小目标 -->
            <div style="background: white; border-radius: 16px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.05);">
                <h3 style="margin: 0 0 12px 0; color: #2c3e50;">🎯 下周小目标</h3>
                <ul style="margin: 0; padding-left: 20px;">
                    ${suggestions.slice(0, 3).map(s => `<li>${s.replace(/建议/, '尝试')}</li>`).join('')}
                    ${suggestions.length === 0 ? '<li>🌟 尝试增加运动，让健康更全面</li>' : ''}
                    <li>📱 睡前1小时不刷手机</li>
                </ul>
            </div>
            
            <div style="text-align: center; margin-top: 16px; font-size: 0.7rem; color: #999;">
                📖 参考依据：《中国居民膳食指南(2022)》 | 数据基于近7日记录
            </div>
        </div>
    `;

    return html;
}

// 刷新报告
async function refreshReport() {
    await loadSevenDayReport();
    showToast('报告已刷新', 'success');
}

// 兼容原有函数
async function loadFusionReport() {
    await loadSevenDayReport();
}

async function loadDailyReport() {
    await loadSevenDayReport();
}

// 挂载全局函数
window.renderReportPanel = renderReportPanel;
window.loadSevenDayReport = loadSevenDayReport;
window.refreshReport = refreshReport;
window.loadFusionReport = loadFusionReport;
window.loadDailyReport = loadDailyReport;