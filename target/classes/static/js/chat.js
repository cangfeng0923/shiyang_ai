// diet.js - 完整修复版（AI报告 + 每日详情）

let currentMeal = 'BREAKFAST';
let foodItems = [];
let editingRecordId = null;
let editingMealKey = null;
let editingMealDate = null;
let deletedRecordIds = [];

// 单位换算映射
const unitConversion = {
    'g': { toGrams: 1, hint: '' },
    'kg': { toGrams: 1000, hint: '1kg = 1000g' },
    'ml': { toGrams: 1, hint: '1ml ≈ 1g（水）' },
    '杯': { toGrams: 200, hint: '1杯 ≈ 200ml/200g' },
    '碗': { toGrams: 250, hint: '1碗 ≈ 250ml/250g' },
    '勺': { toGrams: 15, hint: '1勺 ≈ 15g' },
    '个': { toGrams: 50, hint: '1个约50g（仅供参考）' },
    '片': { toGrams: 10, hint: '1片约10g（仅供参考）' },
    '块': { toGrams: 30, hint: '1块约30g' },
    '盘': { toGrams: 200, hint: '1盘约200g' }
};

let cachedCommonFoods = [];

function getMealName(mealType) {
    const map = {
        'BREAKFAST': '🌅 早餐',
        'LUNCH': '☀️ 午餐',
        'DINNER': '🌙 晚餐',
        'SNACK': '🍪 加餐'
    };
    return map[mealType] || mealType;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatDateTime(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;
}

function calculateGramsFromAmount(amount, unit) {
    if (!amount) return null;
    const conversion = unitConversion[unit] || unitConversion['g'];
    return Math.round(amount * conversion.toGrams);
}

// ========== 常用食物功能 ==========
async function loadCommonFoods() {
    if (!currentUser) return;
    try {
        const response = await fetch(`${API_BASE}/api/diet/common-foods/${currentUser.userId}?limit=8`);
        const data = await response.json();
        if (data.success && data.foods) {
            cachedCommonFoods = data.foods;
        }
    } catch (error) {
        console.error('加载常用食物失败:', error);
        cachedCommonFoods = [];
    }
}

function renderCommonFoodsSection() {
    if (!cachedCommonFoods || cachedCommonFoods.length === 0) return '';

    return `
        <div class="common-foods-section" style="margin: 12px 0; padding: 8px; background: #f8f9fa; border-radius: 12px;">
            <div style="font-size: 0.7rem; color: #888; margin-bottom: 6px;">📌 最近常用</div>
            <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                ${cachedCommonFoods.map(food => `
                    <button type="button" class="common-food-btn" onclick="quickAddFood('${escapeHtml(food.food_name)}', 'g', 1)"
                            style="padding:4px 12px; background:#eef2ff; border:none; border-radius:20px; cursor:pointer;">
                        🍽️ ${escapeHtml(food.food_name)}
                        <span style="font-size: 0.6rem; color: #888;">(${food.count}次)</span>
                    </button>
                `).join('')}
            </div>
        </div>
    `;
}

function quickAddFood(name, unit, amount) {
    const conversion = unitConversion[unit] || unitConversion['g'];
    const grams = amount ? amount * conversion.toGrams : null;

    foodItems.push({
        name: name,
        amount: amount,
        unit: unit,
        grams: grams
    });
    renderFoodItemsList();
}

// ========== 食物列表渲染 ==========
function renderUnitOptions(selectedUnit) {
    const units = ['g', 'kg', 'ml', '杯', '碗', '勺', '个', '片', '块', '盘'];
    return units.map(unit =>
        `<option value="${unit}" ${selectedUnit === unit ? 'selected' : ''}>${unit}</option>`
    ).join('');
}

function getUnitHint(unit) {
    const conversion = unitConversion[unit];
    return conversion && conversion.hint ? `💡 ${conversion.hint}` : '';
}

function renderFoodItemsList() {
    const container = document.getElementById('foodItemsList');
    if (!container) return;

    if (foodItems.length === 0) {
        container.innerHTML = '<div style="text-align:center; padding:20px; color:#999;">点击下方按钮添加食物</div>';
        return;
    }

    container.innerHTML = foodItems.map((item, index) => `
        <div class="food-item" style="border:1px solid #e0e4e8; border-radius:10px; padding:10px; margin-bottom:10px;">
            <div style="display:flex; justify-content:space-between; margin-bottom:8px;">
                <input type="text" placeholder="食物名称" value="${escapeHtml(item.name || '')}" 
                       style="flex:1; padding:8px; border:1px solid #ddd; border-radius:8px;" 
                       onchange="updateFoodItem(${index}, 'name', this.value)">
                <button onclick="removeFoodItem(${index})" 
                        style="background:#fee; border:none; border-radius:8px; padding:0 12px; margin-left:8px; cursor:pointer;">✕</button>
            </div>
            <div style="display:flex; align-items:center; gap:8px;">
                <label style="font-size:0.8rem;">数量</label>
                <input type="number" placeholder="数量" value="${item.amount || ''}" step="0.1" 
                       style="width:80px; padding:8px; border:1px solid #ddd; border-radius:8px;" 
                       onchange="updateFoodItem(${index}, 'amount', parseFloat(this.value) || 0)">
                <select onchange="updateFoodItem(${index}, 'unit', this.value)"
                        style="padding:8px; border:1px solid #ddd; border-radius:8px;">
                    ${renderUnitOptions(item.unit || 'g')}
                </select>
                <span style="font-size:0.7rem; color:#888;">${getUnitHint(item.unit || 'g')}</span>
            </div>
        </div>
    `).join('');
}

function addFoodItem() {
    foodItems.push({ name: '', amount: null, unit: 'g', grams: null });
    renderFoodItemsList();
}

function removeFoodItem(index) {
    if (foodItems.length === 1) {
        foodItems[0] = { name: '', amount: null, unit: 'g', grams: null };
    } else {
        foodItems.splice(index, 1);
    }
    renderFoodItemsList();
}

function updateFoodItem(index, field, value) {
    foodItems[index][field] = value;
    if (field === 'amount' || field === 'unit') {
        const item = foodItems[index];
        const conversion = unitConversion[item.unit] || unitConversion['g'];
        item.grams = item.amount ? item.amount * conversion.toGrams : null;
    }
}

// ========== 餐次选择 ==========
function selectMeal(meal) {
    currentMeal = meal;
    updateMealButtonActive();
}

function updateMealButtonActive() {
    document.querySelectorAll('.meal-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.getAttribute('data-meal') === currentMeal) {
            btn.classList.add('active');
        }
    });
}

function cancelEdit() {
    editingRecordId = null;
    editingMealKey = null;
    editingMealDate = null;
    deletedRecordIds = [];
    foodItems = [];
    addFoodItem();
    renderDietPanel();
}

// ========== 记录提交 ==========
async function submitMealRecords() {
    if (!currentUser) {
        alert('请先登录');
        return;
    }

    const validFoods = foodItems.filter(item => item.name && item.name.trim());
    if (validFoods.length === 0) {
        alert('请至少添加一种食物');
        return;
    }

    const mealNotes = document.getElementById('mealNotes')?.value || '';
    const recordDateTime = formatDateTime(new Date());

    // 编辑模式
    if (editingRecordId && editingMealKey) {
        const recordsToSave = [];

        for (const food of validFoods) {
            const grams = calculateGramsFromAmount(food.amount, food.unit);
            recordsToSave.push({
                id: food.id || null,
                userId: currentUser.userId,
                mealType: currentMeal,
                foodName: food.name.trim(),
                grams: grams,
                originalAmount: food.amount,
                originalUnit: food.unit,
                notes: mealNotes || null,
                recordDate: editingMealDate || recordDateTime
            });
        }

        try {
            const response = await fetch(`${API_BASE}/api/diet/meal-records`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: currentUser.userId,
                    mealType: currentMeal,
                    recordDate: editingMealDate || recordDateTime,
                    records: recordsToSave,
                    deletedIds: deletedRecordIds
                })
            });
            const data = await response.json();
            if (data.success) {
                alert(`更新成功！`);
                cancelEdit();
                await loadTodayRecords();
                await loadWeekReport();
            } else {
                alert('更新失败');
            }
        } catch (error) {
            alert('网络错误');
        }
        return;
    }

    // 新增模式
    let successCount = 0;
    for (const food of validFoods) {
        const grams = calculateGramsFromAmount(food.amount, food.unit);
        try {
            const response = await fetch(`${API_BASE}/api/diet/record`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: currentUser.userId,
                    mealType: currentMeal,
                    foodName: food.name.trim(),
                    grams: grams,
                    originalAmount: food.amount,
                    originalUnit: food.unit,
                    notes: mealNotes || null,
                    recordDate: recordDateTime
                })
            });
            const data = await response.json();
            if (data.success) successCount++;
        } catch (error) {
            console.error('记录失败:', error);
        }
    }

    if (successCount > 0) {
        alert(`记录成功！共记录 ${successCount} 种食物`);
        foodItems = [];
        addFoodItem();
        const notesInput = document.getElementById('mealNotes');
        if (notesInput) notesInput.value = '';
        await loadTodayRecords();
        await loadWeekReport();
        await loadCommonFoods();
    } else {
        alert('记录失败，请重试');
    }
}

// ========== 加载今日记录 ==========
async function loadTodayRecords() {
    if (!currentUser) return;
    const container = document.getElementById('todayRecords');
    if (!container) return;

    try {
        const response = await fetch(`${API_BASE}/api/diet/today/${currentUser.userId}`);
        const data = await response.json();

        if (data.records && data.records.length > 0) {
            const groupedByMeal = {};
            for (const record of data.records) {
                const mealKey = record.mealType;
                if (!groupedByMeal[mealKey]) groupedByMeal[mealKey] = [];
                groupedByMeal[mealKey].push(record);
            }

            let html = '';
            const mealOrder = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
            for (const meal of mealOrder) {
                if (groupedByMeal[meal] && groupedByMeal[meal].length > 0) {
                    html += `<div style="margin-bottom: 16px;">
                        <strong style="color: #667eea;">${getMealName(meal)}</strong>
                        <div style="margin-top: 8px;">`;
                    for (const record of groupedByMeal[meal]) {
                        let scoreClass = 'score-good';
                        if (record.healthScore >= 80) scoreClass = 'score-excellent';
                        if (record.healthScore < 60) scoreClass = 'score-poor';

                        let amountDisplay = '';
                        if (record.originalAmount && record.originalUnit && record.originalUnit !== 'g') {
                            amountDisplay = ` ${record.originalAmount}${record.originalUnit}`;
                        } else if (record.grams) {
                            amountDisplay = ` ${record.grams}g`;
                        }

                        html += `<div style="display:flex; justify-content:space-between; align-items:center; padding:8px; border-bottom:1px solid #eee;">
                            <div style="flex:1">${escapeHtml(record.foodName)}${amountDisplay}</div>
                            <span style="padding:2px 8px; border-radius:20px; background:#f0f2f5; font-weight:bold;">${record.healthScore}</span>
                            <button onclick="editDietRecord('${record.id}')" style="background:none; border:none; cursor:pointer; margin-left:8px;">✏️</button>
                            <button onclick="deleteDietRecord('${record.id}')" style="background:none; border:none; cursor:pointer;">🗑️</button>
                        </div>`;
                    }
                    html += `</div></div>`;
                }
            }
            container.innerHTML = html;
        } else {
            container.innerHTML = '<p style="text-align:center; padding:20px; color:#999;">今日暂无饮食记录</p>';
        }
    } catch (error) {
        container.innerHTML = '<p style="text-align:center; color:#999;">加载失败</p>';
    }
}

// ========== 编辑和删除 ==========
async function editDietRecord(recordId) {
    if (!currentUser) return;

    try {
        const response = await fetch(`${API_BASE}/api/diet/meal-records/${currentUser.userId}?recordId=${recordId}`);
        const data = await response.json();

        if (data.success && data.records) {
            editingRecordId = recordId;
            editingMealKey = data.mealType;
            editingMealDate = data.recordDate;
            currentMeal = data.mealType;
            deletedRecordIds = [];

            foodItems = data.records.map(record => {
                let amount = record.grams;
                let unit = 'g';

                if (record.originalAmount && record.originalUnit) {
                    amount = record.originalAmount;
                    unit = record.originalUnit;
                }

                return {
                    id: record.id,
                    name: record.foodName,
                    amount: amount,
                    unit: unit,
                    grams: record.grams
                };
            });

            if (foodItems.length === 0) addFoodItem();

            const notesInput = document.getElementById('mealNotes');
            if (notesInput) notesInput.value = '';

            renderDietPanel();
        } else {
            alert('获取记录失败');
        }
    } catch (error) {
        console.error('获取记录失败:', error);
        alert('获取记录失败');
    }
}

async function deleteDietRecord(recordId) {
    if (!confirm('确定删除吗？')) return;
    try {
        const response = await fetch(`${API_BASE}/api/diet/record/${recordId}?userId=${currentUser.userId}`, {
            method: 'DELETE'
        });
        const data = await response.json();
        if (data.success) {
            alert('删除成功');
            await loadTodayRecords();
            await loadWeekReport();
        } else {
            alert('删除失败');
        }
    } catch (error) {
        alert('网络错误');
    }
}

// ========== 周报告（AI增强版 - 完整显示每日详情）==========
async function loadWeekReport() {
    if (!currentUser) return;
    const container = document.getElementById('weekReportContent');
    if (!container) return;

    container.innerHTML = '<div style="text-align:center; padding:20px;">🤖 AI正在分析您的饮食报告...</div>';

    try {
        // 调用 AI 报告接口
        const response = await fetch(`${API_BASE}/api/agent/diet-report/${currentUser.userId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ constitution: currentUser.constitution || '平和质' })
        });
        const data = await response.json();

        if (data.success && data.records) {
            // 使用 AI 报告 + 完整每日详情
            const reportHtml = generateFullWeekReport(data.records, data.stats, data.aiReport);
            container.innerHTML = reportHtml;
        } else {
            await loadWeekReportFallback(container);
        }
    } catch (error) {
        console.error('加载AI报告失败:', error);
        await loadWeekReportFallback(container);
    }
}

async function loadWeekReportFallback(container) {
    try {
        const response = await fetch(`${API_BASE}/api/diet/week/${currentUser.userId}`);
        const data = await response.json();
        if (data.records && data.records.length > 0) {
            container.innerHTML = generateFullWeekReport(data.records, null, null);
        } else {
            container.innerHTML = '<p style="text-align:center; padding:20px;">暂无饮食记录</p>';
        }
    } catch (error) {
        container.innerHTML = '<p style="text-align:center; padding:20px;">加载失败，请刷新重试</p>';
    }
}

// 完整周报告生成函数（含每日详情 + AI建议）
function generateFullWeekReport(records, stats, aiReport) {
    // 按日期分组
    const groupedByDate = {};
    for (const record of records) {
        let date = record.recordDate;
        if (date && date.includes('T')) date = date.split('T')[0];
        if (!groupedByDate[date]) groupedByDate[date] = [];
        groupedByDate[date].push(record);
    }

    // 计算统计数据（如果后端没返回）
    let totalScore = 0;
    let totalRecords = records.length;
    let vegCount = 0, proteinCount = 0, breakfastCount = 0;
    let vegGrams = 0, proteinGrams = 0;
    const uniqueFoods = new Set();
    const uniqueDates = new Set();

    for (const record of records) {
        totalScore += record.healthScore || 0;
        const foodName = record.foodName || '';
        const grams = record.grams || 0;

        let date = record.recordDate;
        if (date && date.includes('T')) uniqueDates.add(date.split('T')[0]);

        uniqueFoods.add(foodName);

        // 简单分类统计
        if (foodName.includes('菜') || foodName.includes('瓜')) {
            vegCount++;
            vegGrams += grams;
        }
        if (foodName.includes('肉') || foodName.includes('蛋') || foodName.includes('鱼') || foodName.includes('豆腐')) {
            proteinCount++;
            proteinGrams += grams;
        }
        if (record.mealType === 'BREAKFAST') breakfastCount++;
    }

    const dayCount = uniqueDates.size || 1;
    const avgScore = stats?.avgScore || (totalRecords > 0 ? Math.round(totalScore / totalRecords) : 0);
    const avgVeg = stats ? 0 : Math.round(vegGrams / dayCount);
    const avgProtein = stats ? 0 : Math.round(proteinGrams / dayCount);

    let scoreColor = '#f59e0b', scoreLevel = '良好';
    if (avgScore >= 80) { scoreColor = '#10b981'; scoreLevel = '优秀'; }
    else if (avgScore < 60) { scoreColor = '#ef4444'; scoreLevel = '需改进'; }

    let html = `
        <div style="padding: 8px;">
            <!-- 头部统计 -->
            <div style="text-align: center; margin-bottom: 20px;">
                <h3 style="margin: 0;">📊 近7日饮食报告</h3>
                <p style="color: #666; font-size: 0.8rem;">${new Date().toLocaleDateString()}</p>
            </div>
            
            <!-- 统计卡片 -->
            <div style="display: flex; justify-content: space-around; margin-bottom: 20px; flex-wrap: wrap; gap: 12px;">
                <div style="text-align: center; background: #f0f2f5; padding: 12px 20px; border-radius: 12px;">
                    <div style="font-size: 1.8rem; font-weight: bold; color: ${scoreColor};">${avgScore}</div>
                    <div style="font-size: 0.7rem;">平均评分</div>
                    <div style="font-size: 0.7rem; color: ${scoreColor};">${scoreLevel}</div>
                </div>
                <div style="text-align: center; background: #f0f2f5; padding: 12px 20px; border-radius: 12px;">
                    <div style="font-size: 1.8rem; font-weight: bold;">${totalRecords}</div>
                    <div style="font-size: 0.7rem;">总记录餐数</div>
                </div>
                <div style="text-align: center; background: #f0f2f5; padding: 12px 20px; border-radius: 12px;">
                    <div style="font-size: 1.8rem; font-weight: bold;">${uniqueDates.size}</div>
                    <div style="font-size: 0.7rem;">记录天数</div>
                </div>
            </div>
            
            <!-- 营养分析卡片 -->
            <div style="margin-bottom: 20px; display: flex; gap: 12px; flex-wrap: wrap;">
                <div style="flex:1; background: #f0f2f5; padding: 10px; border-radius: 8px;">
                    <div style="font-size: 0.7rem;">🥬 蔬菜摄入</div>
                    <div style="font-size: 1.2rem; font-weight: bold;">${avgVeg}g/天</div>
                    <div style="font-size: 0.7rem; color: ${avgVeg >= 300 ? '#10b981' : '#f59e0b'};">${avgVeg >= 300 ? '✅ 达标' : '⚠️ 建议增加'}</div>
                </div>
                <div style="flex:1; background: #f0f2f5; padding: 10px; border-radius: 8px;">
                    <div style="font-size: 0.7rem;">🥩 蛋白质摄入</div>
                    <div style="font-size: 1.2rem; font-weight: bold;">${avgProtein}g/天</div>
                    <div style="font-size: 0.7rem; color: ${avgProtein >= 60 ? '#10b981' : '#f59e0b'};">${avgProtein >= 60 ? '✅ 充足' : '⚠️ 建议增加'}</div>
                </div>
                <div style="flex:1; background: #f0f2f5; padding: 10px; border-radius: 8px;">
                    <div style="font-size: 0.7rem;">🌅 早餐次数</div>
                    <div style="font-size: 1.2rem; font-weight: bold;">${breakfastCount}次/周</div>
                    <div style="font-size: 0.7rem; color: ${breakfastCount >= 5 ? '#10b981' : '#f59e0b'};">${breakfastCount >= 5 ? '✅ 规律' : '⚠️ 建议增加'}</div>
                </div>
            </div>
            
            <!-- 每日详情（重点！） -->
            <h4 style="margin-bottom: 12px;">📋 每日详情</h4>
            <div style="max-height: 350px; overflow-y: auto; margin-bottom: 20px;">
    `;

    const sortedDates = Object.keys(groupedByDate).sort().reverse();
    for (const date of sortedDates) {
        const dayRecords = groupedByDate[date];
        const dayAvgScore = Math.round(dayRecords.reduce((s, r) => s + (r.healthScore || 0), 0) / dayRecords.length);

        html += `
            <div style="margin-bottom: 16px; border-left: 3px solid #667eea; padding-left: 12px;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                    <strong>📅 ${date}</strong>
                    <span style="background: #dbeafe; padding: 2px 8px; border-radius: 20px; font-size: 0.7rem;">评分 ${dayAvgScore}</span>
                </div>
        `;

        for (const record of dayRecords) {
            const mealName = getMealName(record.mealType);
            let amountDisplay = '';
            if (record.originalAmount && record.originalUnit && record.originalUnit !== 'g') {
                amountDisplay = ` ${record.originalAmount}${record.originalUnit}`;
            } else if (record.grams) {
                amountDisplay = ` ${record.grams}g`;
            }

            html += `
                <div style="font-size: 0.85rem; padding: 4px 0; display: flex; justify-content: space-between;">
                    <span>${mealName}：${escapeHtml(record.foodName)}${amountDisplay}</span>
                    <span style="color: ${record.healthScore >= 80 ? '#10b981' : (record.healthScore >= 60 ? '#f59e0b' : '#ef4444')};">${record.healthScore}分</span>
                </div>
            `;
        }
        html += `</div>`;
    }

    // AI 饮食建议
    let adviceHtml = '';
    if (aiReport) {
        adviceHtml = `
            <div style="margin-top: 20px; padding: 12px; background: #eef2ff; border-radius: 8px;">
                <div style="font-size: 0.8rem; color: #667eea; margin-bottom: 8px;">🤖 AI饮食建议</div>
                <div style="font-size: 0.85rem; line-height: 1.5;">${aiReport.replace(/\n/g, '<br>')}</div>
            </div>
        `;
    } else {
        // 降级建议
        let fallbackAdvice = '';
        if (avgVeg < 300) fallbackAdvice += '🥬 蔬菜摄入不足，建议每天吃够300g绿叶蔬菜。<br>';
        if (avgProtein < 60) fallbackAdvice += '🥩 蛋白质摄入不足，建议增加鸡蛋、鱼肉、豆腐。<br>';
        if (breakfastCount < 5) fallbackAdvice += '🌅 早餐次数偏少，规律吃早餐有助于全天代谢。<br>';
        if (uniqueFoods.size < 12) fallbackAdvice += '🍽️ 食物种类偏少，建议每周吃够12种以上不同食物。<br>';
        if (!fallbackAdvice) fallbackAdvice = '👍 饮食结构良好，继续保持！';

        adviceHtml = `
            <div style="margin-top: 20px; padding: 12px; background: #eef2ff; border-radius: 8px;">
                <div style="font-size: 0.8rem; color: #667eea; margin-bottom: 8px;">💡 饮食建议</div>
                <div style="font-size: 0.85rem; line-height: 1.5;">${fallbackAdvice}</div>
            </div>
        `;
    }

    html += `</div>${adviceHtml}</div>`;

    return html;
}

// ========== 主渲染函数 ==========
async function renderDietPanel() {
    const panel = document.getElementById('diet-panel');
    if (!panel) return;

    await loadCommonFoods();

    panel.innerHTML = `
        <div style="background:white; border-radius:16px; padding:16px; margin-bottom:20px;">
            <h4 style="margin:0 0 12px 0;">🍽️ ${editingRecordId ? '编辑饮食' : '记录今日饮食'}</h4>
            ${editingRecordId ? '<div style="background:#eef2ff; padding:8px; border-radius:8px; margin-bottom:12px;">✏️ 正在编辑模式</div>' : ''}
            
            <div style="display:flex; gap:10px; margin-bottom:16px;">
                <button class="meal-btn" data-meal="BREAKFAST" onclick="selectMeal('BREAKFAST')" style="flex:1; padding:10px; border:none; border-radius:25px; cursor:pointer;">🌅 早餐</button>
                <button class="meal-btn" data-meal="LUNCH" onclick="selectMeal('LUNCH')" style="flex:1; padding:10px; border:none; border-radius:25px; cursor:pointer;">☀️ 午餐</button>
                <button class="meal-btn" data-meal="DINNER" onclick="selectMeal('DINNER')" style="flex:1; padding:10px; border:none; border-radius:25px; cursor:pointer;">🌙 晚餐</button>
                <button class="meal-btn" data-meal="SNACK" onclick="selectMeal('SNACK')" style="flex:1; padding:10px; border:none; border-radius:25px; cursor:pointer;">🍪 加餐</button>
            </div>
            
            ${renderCommonFoodsSection()}
            
            <div id="foodListContainer">
                <div id="foodItemsList"></div>
            </div>
            
            <button onclick="addFoodItem()" style="width:100%; padding:12px; background:#f0f2f5; border:none; border-radius:12px; margin:10px 0; cursor:pointer;">+ 添加食物</button>
            
            <textarea id="mealNotes" rows="2" placeholder="餐次备注（可选）" style="width:100%; padding:10px; border-radius:10px; border:1px solid #e0e4e8; resize:none;"></textarea>
            
            <div style="display: flex; gap: 10px; margin-top: 12px;">
                <button onclick="submitMealRecords()" style="flex:1; padding:12px; background:#667eea; color:white; border:none; border-radius:12px; cursor:pointer;">📝 ${editingRecordId ? '保存修改' : '记录本餐'}</button>
                ${editingRecordId ? '<button onclick="cancelEdit()" style="padding:12px 20px; background:#e0e4e8; border:none; border-radius:12px; cursor:pointer;">取消编辑</button>' : ''}
            </div>
        </div>
        
        <div style="background:white; border-radius:16px; padding:16px; margin-bottom:20px;">
            <h4 style="margin:0 0 12px 0;">📅 今日饮食</h4>
            <div id="todayRecords"></div>
        </div>
        
        <div style="background:white; border-radius:16px; padding:16px;">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:16px;">
                <h4 style="margin:0;">📊 近7日饮食报告</h4>
                <button onclick="loadWeekReport()" style="padding:6px 12px; background:#f0f2f5; border:none; border-radius:20px; cursor:pointer;">🔄 刷新报告</button>
            </div>
            <div id="weekReportContent">加载中...</div>
        </div>
    `;

    if (foodItems.length === 0 && !editingRecordId) {
        addFoodItem();
    } else if (foodItems.length > 0) {
        renderFoodItemsList();
    }

    updateMealButtonActive();
    await loadTodayRecords();
    await loadWeekReport();
}

// 挂载全局函数
window.renderDietPanel = renderDietPanel;
window.selectMeal = selectMeal;
window.addFoodItem = addFoodItem;
window.removeFoodItem = removeFoodItem;
window.updateFoodItem = updateFoodItem;
window.submitMealRecords = submitMealRecords;
window.loadWeekReport = loadWeekReport;
window.editDietRecord = editDietRecord;
window.deleteDietRecord = deleteDietRecord;
window.cancelEdit = cancelEdit;
window.quickAddFood = quickAddFood;