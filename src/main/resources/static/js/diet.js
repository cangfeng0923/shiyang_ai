// diet.js - 添加删除和修改功能的完整版
// ========== 确保全局变量存在 ==========
if (typeof API_BASE === 'undefined') {
    var API_BASE = '';  // 使用相对路径
}

if (typeof currentUser === 'undefined') {
    var currentUser = null;
}

let currentMeal = 'BREAKFAST';
let foodItems = [];
let editingRecordId = null;  // 正在编辑的记录ID

// 确保 getMealName 函数存在
function getMealName(mealType) {
    const map = {
        'BREAKFAST': '🌅 早餐',
        'LUNCH': '☀️ 午餐',
        'DINNER': '🌙 晚餐',
        'SNACK': '🍪 加餐'
    };
    return map[mealType] || mealType;
}

// 单位换算映射
const unitConversion = {
    'g': { toGrams: 1, hint: '' },
    'kg': { toGrams: 1000, hint: '1kg = 1000g' },
    'ml': { toGrams: 1, hint: '1ml ≈ 1g（水）' },
    '杯': { toGrams: 200, hint: '1杯 ≈ 200ml/200g' },
    '碗': { toGrams: 250, hint: '1碗 ≈ 250ml/250g' },
    '勺': { toGrams: 15, hint: '1勺 ≈ 15g' },
    '个': { toGrams: 50, hint: '1个约50g（仅供参考）' },
    '片': { toGrams: 10, hint: '1片约10g（仅供参考）' }
};

function renderDietPanel() {
    const panel = document.getElementById('diet-panel');
    if (!panel) return;

    panel.innerHTML = `
        <div class="form-card">
            <h4>🍽️ ${editingRecordId ? '编辑饮食' : '记录今日饮食'}</h4>
            ${editingRecordId ? '<div style="background:#eef2ff; padding:8px; border-radius:8px; margin-bottom:12px;">✏️ 正在编辑模式</div>' : ''}
            <div class="meal-buttons">
                <button class="meal-btn" data-meal="BREAKFAST" onclick="selectMeal('BREAKFAST')">🌅 早餐</button>
                <button class="meal-btn" data-meal="LUNCH" onclick="selectMeal('LUNCH')">☀️ 午餐</button>
                <button class="meal-btn" data-meal="DINNER" onclick="selectMeal('DINNER')">🌙 晚餐</button>
                <button class="meal-btn" data-meal="SNACK" onclick="selectMeal('SNACK')">🍪 加餐</button>
            </div>
            
            <div class="food-list-container" id="foodListContainer">
                <div id="foodItemsList"></div>
            </div>
            
            <button class="add-food-btn" onclick="addFoodItem()">+ 添加食物</button>
            
            <div class="form-row">
                <textarea id="mealNotes" rows="2" placeholder="餐次备注（可选）" style="width:100%; padding:10px; border-radius:10px; border:1px solid #e0e4e8; resize:none;"></textarea>
            </div>
            
            <div style="display: flex; gap: 10px;">
                <button class="btn-primary" onclick="submitMealRecords()">📝 ${editingRecordId ? '保存修改' : '记录本餐'}</button>
                ${editingRecordId ? '<button class="btn-secondary" onclick="cancelEdit()">取消编辑</button>' : ''}
            </div>
        </div>
        
        <div class="form-card">
            <h4>📅 今日饮食</h4>
            <div id="todayRecords" class="record-list"></div>
        </div>
        
        <div class="report-card">
            <div class="report-header">
                <h4>📊 近7日饮食报告</h4>
                <button class="refresh-report-btn" onclick="loadWeekReport()">🔄 刷新报告</button>
            </div>
            <div id="weekReportContent" class="week-report-content">
                <div class="loading-spinner"></div> 加载中...
            </div>
        </div>
    `;

    if (foodItems.length === 0 && !editingRecordId) {
        addFoodItem();
    } else if (foodItems.length > 0) {
        renderFoodItemsList();
    }

    updateMealButtonActive();
    loadTodayRecords();
    loadWeekReport();
}

function renderFoodItemsList() {
    const container = document.getElementById('foodItemsList');
    if (!container) return;

    container.innerHTML = foodItems.map((item, index) => `
        <div class="food-item" data-index="${index}">
            <div class="food-item-header">
                <input type="text" class="food-name-input" placeholder="食物名称" value="${escapeHtml(item.name || '')}" onchange="updateFoodItem(${index}, 'name', this.value)">
                <button class="remove-food-btn" onclick="removeFoodItem(${index})">✕</button>
            </div>
            <div class="food-item-details">
                <div class="amount-input-group">
                    <label>数量</label>
                    <div class="amount-input-wrapper">
                        <input type="number" class="amount-value" placeholder="数量" value="${item.amount || ''}" step="0.1" onchange="updateFoodItem(${index}, 'amount', parseFloat(this.value) || 0)">
                        <select class="unit-select" onchange="updateFoodItem(${index}, 'unit', this.value)">
                            ${renderUnitOptions(item.unit || 'g')}
                        </select>
                    </div>
                    <span class="unit-hint" id="unitHint_${index}">${getUnitHint(item.unit || 'g')}</span>
                </div>
            </div>
        </div>
    `).join('');

    foodItems.forEach((item, index) => {
        const hintSpan = document.getElementById(`unitHint_${index}`);
        if (hintSpan) {
            hintSpan.textContent = getUnitHint(item.unit || 'g');
        }
    });
}

function renderUnitOptions(selectedUnit) {
    const units = ['g', 'kg', 'ml', '杯', '碗', '勺', '个', '片'];
    return units.map(unit =>
        `<option value="${unit}" ${selectedUnit === unit ? 'selected' : ''}>${unit}</option>`
    ).join('');
}

function getUnitHint(unit) {
    const conversion = unitConversion[unit];
    if (conversion && conversion.hint) {
        return `💡 ${conversion.hint}`;
    }
    return '';
}

function addFoodItem() {
    foodItems.push({
        name: '',
        amount: null,
        unit: 'g',
        grams: null
    });
    renderFoodItemsList();
}

function removeFoodItem(index) {
    if (foodItems.length === 1) {
        foodItems[0] = { name: '', amount: null, unit: 'g', grams: null };
        renderFoodItemsList();
    } else {
        foodItems.splice(index, 1);
        renderFoodItemsList();
    }
}

function updateFoodItem(index, field, value) {
    foodItems[index][field] = value;

    if (field === 'amount' || field === 'unit') {
        const item = foodItems[index];
        const conversion = unitConversion[item.unit] || unitConversion['g'];
        if (item.amount) {
            item.grams = item.amount * conversion.toGrams;
        } else {
            item.grams = null;
        }
    }
}

function calculateGramsFromAmount(amount, unit) {
    if (!amount) return null;
    const conversion = unitConversion[unit] || unitConversion['g'];
    return Math.round(amount * conversion.toGrams);
}

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
    foodItems = [];
    addFoodItem();
    renderDietPanel();
}

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
    const now = new Date();
    const recordDateTime = formatDateTime(now);

    // 如果是编辑模式，更新记录
    if (editingRecordId) {
        const food = validFoods[0];
        const grams = calculateGramsFromAmount(food.amount, food.unit);

        const record = {
            id: editingRecordId,
            userId: currentUser.userId,
            mealType: currentMeal,
            foodName: food.name.trim(),
            grams: grams,
            notes: mealNotes || null,
            recordDate: recordDateTime
        };

        try {
            const response = await fetch(`${API_BASE}/api/diet/record/${editingRecordId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(record)
            });
            const data = await response.json();

            if (data.success) {
                alert(`更新成功！健康评分：${data.healthScore}/100`);
                editingRecordId = null;
                foodItems = [];
                addFoodItem();
                document.getElementById('mealNotes').value = '';
                await loadTodayRecords();
                await loadWeekReport();
            } else {
                alert('更新失败');
            }
        } catch (error) {
            console.error('更新失败:', error);
            alert('网络错误: ' + error.message);
        }
        return;
    }

    // 新增模式
    let successCount = 0;
    let failedFoods = [];

    for (const food of validFoods) {
        const grams = calculateGramsFromAmount(food.amount, food.unit);

        const record = {
            userId: currentUser.userId,
            mealType: currentMeal,
            foodName: food.name.trim(),
            grams: grams,
            notes: mealNotes || null,
            recordDate: recordDateTime
        };

        try {
            const response = await fetch(`${API_BASE}/api/diet/record`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(record)
            });
            const data = await response.json();

            if (data.success) {
                successCount++;
            } else {
                failedFoods.push(food.name);
            }
        } catch (error) {
            console.error('记录失败:', error);
            failedFoods.push(food.name);
        }
    }

    if (successCount > 0) {
        alert(`记录成功！共记录 ${successCount} 种食物${failedFoods.length > 0 ? `，失败: ${failedFoods.join(', ')}` : ''}`);

        foodItems = [];
        addFoodItem();
        document.getElementById('mealNotes').value = '';

        await loadTodayRecords();
        await loadWeekReport();
        showUnitConversionTip();
    } else {
        alert('记录失败，请重试');
    }
}

async function editDietRecord(recordId) {
    if (!currentUser) return;

    try {
        const response = await fetch(`${API_BASE}/api/diet/record/${recordId}`);
        if (!response.ok) {
            // 如果后端没有提供单独的获取接口，我们从今日记录中获取
            const todayResponse = await fetch(`${API_BASE}/api/diet/today/${currentUser.userId}`);
            const todayData = await todayResponse.json();
            const record = todayData.records.find(r => r.id === recordId);
            if (record) {
                populateEditForm(record);
            }
        } else {
            const data = await response.json();
            populateEditForm(data.record);
        }
    } catch (error) {
        console.error('获取记录失败:', error);
        // 从今日记录中获取
        const todayResponse = await fetch(`${API_BASE}/api/diet/today/${currentUser.userId}`);
        const todayData = await todayResponse.json();
        const record = todayData.records.find(r => r.id === recordId);
        if (record) {
            populateEditForm(record);
        } else {
            alert('获取记录失败');
        }
    }
}

function populateEditForm(record) {
    editingRecordId = record.id;
    currentMeal = record.mealType;

    foodItems = [{
        name: record.foodName,
        amount: record.grams || null,
        unit: 'g',
        grams: record.grams
    }];

    document.getElementById('mealNotes').value = record.notes || '';

    renderDietPanel();
}

async function deleteDietRecord(recordId) {
    if (!currentUser) return;

    if (!confirm('确定要删除这条饮食记录吗？')) return;

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
            alert('删除失败: ' + (data.message || '未知错误'));
        }
    } catch (error) {
        console.error('删除失败:', error);
        alert('网络错误: ' + error.message);
    }
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

function showUnitConversionTip() {
    const existingTip = document.querySelector('.unit-conversion-tip');
    if (existingTip) existingTip.remove();

    const tip = document.createElement('div');
    tip.className = 'unit-conversion-tip';
    tip.innerHTML = '💡 小提示：1杯 ≈ 200ml/200g，1碗 ≈ 250ml/250g，1勺 ≈ 15g';
    document.body.appendChild(tip);

    setTimeout(() => {
        tip.remove();
    }, 3000);
}

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
                if (!groupedByMeal[mealKey]) {
                    groupedByMeal[mealKey] = [];
                }
                groupedByMeal[mealKey].push(record);
            }

            let html = '';
            const mealOrder = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
            for (const meal of mealOrder) {
                if (groupedByMeal[meal] && groupedByMeal[meal].length > 0) {
                    html += `<div style="margin-bottom: 16px;">
                        <strong style="font-size: 0.9rem; color: #667eea;">${getMealName(meal)}</strong>
                        <div style="margin-top: 8px;">`;
                    for (const record of groupedByMeal[meal]) {
                        let scoreClass = 'score-good';
                        if (record.healthScore >= 80) scoreClass = 'score-excellent';
                        if (record.healthScore < 60) scoreClass = 'score-poor';

                        html += `<div class="record-item" style="margin-bottom: 8px;">
                            <div style="flex:1">
                                ${escapeHtml(record.foodName)} ${record.grams ? record.grams + 'g' : ''}
                                ${record.notes ? '<br><small style="color:#999;">📝 ' + escapeHtml(record.notes) + '</small>' : ''}
                            </div>
                            <div style="display: flex; gap: 8px; align-items: center;">
                                <div class="${scoreClass}" style="font-weight:bold; min-width:40px; text-align:center;">
                                    ${record.healthScore}
                                </div>
                                <button class="edit-record-btn" onclick="editDietRecord('${record.id}')" title="编辑">✏️</button>
                                <button class="delete-record-btn" onclick="deleteDietRecord('${record.id}')" title="删除">🗑️</button>
                            </div>
                        </div>`;
                    }
                    html += `</div></div>`;
                }
            }
            container.innerHTML = html;
        } else {
            container.innerHTML = '<p style="color:#999; text-align:center; padding:20px;">今日暂无饮食记录</p>';
        }
    } catch (error) {
        console.error('加载饮食记录失败:', error);
        container.innerHTML = '<p style="color:#999; text-align:center;">加载失败</p>';
    }
}

async function loadWeekReport() {
    if (!currentUser) return;

    const container = document.getElementById('weekReportContent');
    if (!container) return;

    container.innerHTML = '<div class="loading-spinner"></div> 加载中...';

    try {
        const response = await fetch(`${API_BASE}/api/diet/week/${currentUser.userId}`);
        const data = await response.json();

        if (data.records && data.records.length > 0) {
            const reportHtml = generateWeekReportHtml(data.records);
            container.innerHTML = reportHtml;
        } else {
            container.innerHTML = '<p style="color:#999; text-align:center; padding:20px;">暂无饮食记录，请先记录您的饮食</p>';
        }
    } catch (error) {
        console.error('加载周报告失败:', error);
        container.innerHTML = '<p style="color:#999; text-align:center;">加载失败，请检查网络连接</p>';
    }
}

// 替换 generateWeekReportHtml 函数
function generateWeekReportHtml(records) {
    const groupedByDate = {};

    // ========== 第一步：计算各项营养指标 ==========
    let totalScore = 0;
    let totalRecords = records.length;
    let vegetableCount = 0;
    let proteinCount = 0;
    let breakfastCount = 0;

    // 新增的营养指标变量
    let totalVegetablesGrams = 0;
    let totalFruitGrams = 0;
    let totalProteinGrams = 0;
    let totalFiberGrams = 0;
    let totalWaterMl = 0;
    const uniqueFoods = new Set();

    // 记录天数
    const uniqueDates = new Set();

    for (const record of records) {
        // 统计原有指标
        totalScore += record.healthScore || 0;
        const foodName = record.foodName || '';
        const grams = record.grams || 0;

        // 记录唯一日期
        let date = record.recordDate;
        if (date && date.includes('T')) {
            date = date.split('T')[0];
        } else if (date) {
            date = date.toString().split(' ')[0];
        }
        if (date && date !== '未知日期') {
            uniqueDates.add(date);
        }

        // 记录唯一食物
        uniqueFoods.add(foodName);

        // 蔬菜识别和克数统计
        if (foodName.includes('蔬菜') || foodName.includes('青菜') || foodName.includes('菜') ||
            foodName.includes('西兰花') || foodName.includes('菠菜') || foodName.includes('白菜') ||
            foodName.includes('西红柿') || foodName.includes('黄瓜') || foodName.includes('萝卜')) {
            vegetableCount++;
            totalVegetablesGrams += grams;
        }

        // 水果识别
        if (foodName.includes('苹果') || foodName.includes('梨') || foodName.includes('香蕉') ||
            foodName.includes('橙子') || foodName.includes('猕猴桃') || foodName.includes('草莓') ||
            foodName.includes('葡萄') || foodName.includes('西瓜')) {
            totalFruitGrams += grams;
        }

        // 蛋白质识别和克数统计
        if (foodName.includes('肉') || foodName.includes('蛋') || foodName.includes('鱼') ||
            foodName.includes('豆腐') || foodName.includes('虾') || foodName.includes('鸡') ||
            foodName.includes('牛奶') || foodName.includes('豆浆')) {
            proteinCount++;
            // 简单估算蛋白质含量（实际克数 * 0.2 约等于蛋白质克数）
            totalProteinGrams += grams * 0.2;
        }

        // 膳食纤维估算（蔬菜水果含纤维较多）
        if (foodName.includes('蔬菜') || foodName.includes('青菜') || foodName.includes('西兰花') ||
            foodName.includes('菠菜') || foodName.includes('苹果') || foodName.includes('香蕉') ||
            foodName.includes('燕麦') || foodName.includes('玉米') || foodName.includes('粗粮')) {
            totalFiberGrams += grams * 0.03; // 假设纤维含量约3%
        }

        // 水分估算
        if (foodName.includes('水') || foodName.includes('汤') || foodName.includes('牛奶') ||
            foodName.includes('豆浆') || foodName.includes('茶') || foodName.includes('咖啡')) {
            totalWaterMl += grams * 0.9;
        }

        if (record.mealType === 'BREAKFAST') {
            breakfastCount++;
        }

        // 按日期分组
        let recordDate = record.recordDate;
        if (recordDate && recordDate.includes('T')) {
            recordDate = recordDate.split('T')[0];
        } else if (recordDate) {
            recordDate = recordDate.toString().split(' ')[0];
        } else {
            recordDate = '未知日期';
        }

        if (!groupedByDate[recordDate]) {
            groupedByDate[recordDate] = [];
        }
        groupedByDate[recordDate].push(record);
    }

    const dayCount = uniqueDates.size || 1;
    const avgScore = totalRecords > 0 ? (totalScore / totalRecords).toFixed(0) : 0;
    const avgVegetables = totalVegetablesGrams / dayCount;
    const avgFruit = totalFruitGrams / dayCount;
    const avgProtein = totalProteinGrams / dayCount;
    const avgFiber = totalFiberGrams / dayCount;
    const avgWater = totalWaterMl / dayCount;
    const foodTypeCount = uniqueFoods.size;

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

    // 计算蔬菜、蛋白质、纤维的达标状态
    const vegStatus = avgVegetables >= 300 ? '✅ 达标' : '⚠️ 建议增加';
    const proteinStatus = avgProtein >= 60 ? '✅ 充足' : '⚠️ 建议增加';
    const fiberStatus = avgFiber >= 25 ? '✅ 充足' : '⚠️ 建议增加';
    const fruitStatus = avgFruit >= 200 ? '✅ 达标' : '⚠️ 建议增加';

    let html = `
        <div style="padding: 8px;">
            <div style="text-align: center; margin-bottom: 20px;">
                <span style="font-size: 2rem;">📊</span>
                <h3 style="margin: 8px 0 4px;">近7日饮食报告</h3>
                <p style="color: #666; font-size: 0.8rem;">${new Date().toLocaleDateString()}</p>
            </div>
            
            <div style="display: flex; justify-content: space-around; margin-bottom: 24px; flex-wrap: wrap; gap: 12px;">
                <div style="text-align: center; background: #f0f2f5; padding: 12px 20px; border-radius: 12px;">
                    <div style="font-size: 1.8rem; font-weight: bold; color: ${scoreColor};">${avgScore}</div>
                    <div style="font-size: 0.7rem; color: #666;">平均健康评分</div>
                    <div style="font-size: 0.8rem; margin-top: 4px;">${scoreLevel}</div>
                </div>
                <div style="text-align: center; background: #f0f2f5; padding: 12px 20px; border-radius: 12px;">
                    <div style="font-size: 1.8rem; font-weight: bold; color: #667eea;">${totalRecords}</div>
                    <div style="font-size: 0.7rem; color: #666;">总记录餐数</div>
                </div>
                <div style="text-align: center; background: #f0f2f5; padding: 12px 20px; border-radius: 12px;">
                    <div style="font-size: 1.8rem; font-weight: bold; color: #667eea;">${Object.keys(groupedByDate).length}</div>
                    <div style="font-size: 0.7rem; color: #666;">记录天数</div>
                </div>
            </div>
            
            <div style="margin-bottom: 20px;">
                <h4 style="margin-bottom: 12px;">📈 营养分析</h4>
                <div style="display: flex; gap: 16px; flex-wrap: wrap;">
                    <div style="flex: 1; background: #f0f2f5; padding: 10px; border-radius: 8px;">
                        <div style="font-size: 0.7rem; color: #666;">🥬 蔬菜摄入</div>
                        <div style="font-size: 1.2rem; font-weight: bold;">${avgVegetables.toFixed(0)}g/天</div>
                        <div style="font-size: 0.7rem; color: ${avgVegetables >= 300 ? '#10b981' : '#f59e0b'};">${vegStatus}</div>
                    </div>
                    <div style="flex: 1; background: #f0f2f5; padding: 10px; border-radius: 8px;">
                        <div style="font-size: 0.7rem; color: #666;">🍎 水果摄入</div>
                        <div style="font-size: 1.2rem; font-weight: bold;">${avgFruit.toFixed(0)}g/天</div>
                        <div style="font-size: 0.7rem; color: ${avgFruit >= 200 ? '#10b981' : '#f59e0b'};">${fruitStatus}</div>
                    </div>
                    <div style="flex: 1; background: #f0f2f5; padding: 10px; border-radius: 8px;">
                        <div style="font-size: 0.7rem; color: #666;">🥩 蛋白质摄入</div>
                        <div style="font-size: 1.2rem; font-weight: bold;">${avgProtein.toFixed(0)}g/天</div>
                        <div style="font-size: 0.7rem; color: ${avgProtein >= 60 ? '#10b981' : '#f59e0b'};">${proteinStatus}</div>
                    </div>
                    <div style="flex: 1; background: #f0f2f5; padding: 10px; border-radius: 8px;">
                        <div style="font-size: 0.7rem; color: #666;">🌾 膳食纤维</div>
                        <div style="font-size: 1.2rem; font-weight: bold;">${avgFiber.toFixed(0)}g/天</div>
                        <div style="font-size: 0.7rem; color: ${avgFiber >= 25 ? '#10b981' : '#f59e0b'};">${fiberStatus}</div>
                    </div>
                </div>
            </div>
            
            <div style="margin-bottom: 20px;">
                <h4 style="margin-bottom: 12px;">🥗 膳食指南对比（中国居民膳食指南2022）</h4>
                <div style="background: #f8f9fc; border-radius: 8px; overflow: hidden;">
                    <table style="width: 100%; border-collapse: collapse; font-size: 0.8rem;">
                        <thead>
                            <tr style="background: #eef2ff;">
                                <th style="padding: 8px; text-align: left;">指标</th>
                                <th style="padding: 8px; text-align: left;">实际摄入</th>
                                <th style="padding: 8px; text-align: left;">推荐值</th>
                                <th style="padding: 8px; text-align: center;">状态</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr style="border-bottom: 1px solid #e2e8f0;">
                                <td style="padding: 8px;">🥬 蔬菜</td>
                                <td style="padding: 8px;">${avgVegetables.toFixed(0)}g/天</td>
                                <td style="padding: 8px;">300-500g/天</td>
                                <td style="padding: 8px; text-align: center;">${avgVegetables >= 300 ? '✅' : '⚠️'}</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #e2e8f0;">
                                <td style="padding: 8px;">🍎 水果</td>
                                <td style="padding: 8px;">${avgFruit.toFixed(0)}g/天</td>
                                <td style="padding: 8px;">200-350g/天</td>
                                <td style="padding: 8px; text-align: center;">${avgFruit >= 200 ? '✅' : '⚠️'}</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #e2e8f0;">
                                <td style="padding: 8px;">🥩 蛋白质</td>
                                <td style="padding: 8px;">${avgProtein.toFixed(0)}g/天</td>
                                <td style="padding: 8px;">60-75g/天</td>
                                <td style="padding: 8px; text-align: center;">${avgProtein >= 60 ? '✅' : '⚠️'}</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #e2e8f0;">
                                <td style="padding: 8px;">🌾 膳食纤维</td>
                                <td style="padding: 8px;">${avgFiber.toFixed(0)}g/天</td>
                                <td style="padding: 8px;">25-30g/天</td>
                                <td style="padding: 8px; text-align: center;">${avgFiber >= 25 ? '✅' : '⚠️'}</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px;">🍽️ 食物多样性</td>
                                <td style="padding: 8px;">${foodTypeCount}种/周</td>
                                <td style="padding: 8px;">≥12种/周</td>
                                <td style="padding: 8px; text-align: center;">${foodTypeCount >= 12 ? '✅' : '⚠️'}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            
            <h4 style="margin-bottom: 12px;">📋 每日详情</h4>
            <div style="max-height: 300px; overflow-y: auto;">
    `;

    const sortedDates = Object.keys(groupedByDate).sort().reverse();
    for (const date of sortedDates) {
        const dayRecords = groupedByDate[date];
        const dayTotalScore = dayRecords.reduce((sum, r) => sum + (r.healthScore || 0), 0);
        const dayAvgScore = (dayTotalScore / dayRecords.length).toFixed(0);

        // 计算当日蔬菜摄入
        let dayVegGrams = 0;
        for (const record of dayRecords) {
            const foodName = record.foodName || '';
            if (foodName.includes('蔬菜') || foodName.includes('青菜') || foodName.includes('菜')) {
                dayVegGrams += record.grams || 0;
            }
        }

        html += `
            <div style="margin-bottom: 16px; border-left: 3px solid #667eea; padding-left: 12px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; flex-wrap: wrap; gap: 8px;">
                    <strong>📅 ${date}</strong>
                    <span style="display: inline-block; padding: 2px 8px; border-radius: 20px; font-size: 0.7rem; font-weight: 500; background: #dbeafe; color: #1e40af;">评分 ${dayAvgScore}</span>
                    ${dayVegGrams > 0 ? `<span style="font-size: 0.7rem; color: #666;">🥬 ${dayVegGrams}g蔬菜</span>` : ''}
                </div>
        `;

        for (const record of dayRecords) {
            const mealName = getMealName(record.mealType);
            html += `
                <div style="font-size: 0.85rem; padding: 4px 0; color: #555; display: flex; justify-content: space-between;">
                    <span>${mealName}：${escapeHtml(record.foodName)} ${record.grams ? record.grams + 'g' : ''}</span>
                    <span style="font-size: 0.7rem; color: ${record.healthScore >= 80 ? '#10b981' : (record.healthScore >= 60 ? '#f59e0b' : '#ef4444')};">${record.healthScore}分</span>
                </div>
            `;
        }

        html += `</div>`;
    }

    html += `
            </div>
            
            <div style="margin-top: 20px; padding: 12px; background: #eef2ff; border-radius: 8px;">
                <div style="font-size: 0.8rem; color: #667eea;">💡 饮食建议</div>
                <div style="font-size: 0.8rem; margin-top: 8px;">
                    ${generateAdviceTextEnhanced(avgScore, avgVegetables, avgFruit, avgProtein, avgFiber, breakfastCount, foodTypeCount)}
                </div>
            </div>
        </div>
    `;

    // 计算雷达图数据百分比
    const vegPercent = Math.min(100, (avgVegetables / 300 * 100)).toFixed(0);
    const fruitPercent = Math.min(100, (avgFruit / 200 * 100)).toFixed(0);
    const proteinPercent = Math.min(100, (avgProtein / 60 * 100)).toFixed(0);
    const fiberPercent = Math.min(100, (avgFiber / 25 * 100)).toFixed(0);
    const waterPercent = Math.min(100, (avgWater / 1500 * 100)).toFixed(0);
    const diversityPercent = Math.min(100, (foodTypeCount / 12 * 100)).toFixed(0);

    // 生成唯一ID用于雷达图
    const radarChartId = 'radarChart_' + Date.now();

    // 雷达图HTML部分 - 替换原来的JSON显示
    html += `
        <div style="margin-top: 20px; padding: 12px; background: #f0f2f5; border-radius: 12px;">
            <h4 style="margin-bottom: 12px;">📐 膳食均衡雷达图</h4>
            <div style="position: relative; height: 280px; width: 100%; margin: 0 auto;">
                <canvas id="${radarChartId}" style="max-height: 250px; width: 100%;"></canvas>
            </div>
            <div style="display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; margin-top: 12px; font-size: 0.7rem;">
                <div><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #667eea; margin-right: 4px;"></span> 蔬菜 300g/天</div>
                <div><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #f59e0b; margin-right: 4px;"></span> 水果 200g/天</div>
                <div><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #10b981; margin-right: 4px;"></span> 蛋白质 60g/天</div>
                <div><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #ef4444; margin-right: 4px;"></span> 膳食纤维 25g/天</div>
                <div><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #8b5cf6; margin-right: 4px;"></span> 饮水 1500ml/天</div>
                <div><span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: #ec489a; margin-right: 4px;"></span> 食物多样性 12种/周</div>
            </div>
            <div style="font-size: 0.7rem; color: #666; margin-top: 12px; text-align: center;">
                💡 雷达图值 = 实际摄入/推荐值 × 100%<br>
                📖 参考依据：《中国居民膳食指南(2022)》
            </div>
        </div>
    `;

    // 返回HTML后，延迟绘制雷达图
    setTimeout(() => {
        drawRadarChart(radarChartId, vegPercent, fruitPercent, proteinPercent, fiberPercent, waterPercent, diversityPercent);
    }, 100);

    return html;
}

// 绘制雷达图的函数
function drawRadarChart(chartId, veg, fruit, protein, fiber, water, diversity) {
    const canvas = document.getElementById(chartId);
    if (!canvas) return;

    // 如果已经存在图表实例，先销毁
    if (canvas.chart) {
        canvas.chart.destroy();
    }

    const ctx = canvas.getContext('2d');

    canvas.chart = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: ['蔬菜', '水果', '蛋白质', '膳食纤维', '饮水', '食物多样性'],
            datasets: [{
                label: '您的摄入达标率',
                data: [veg, fruit, protein, fiber, water, diversity],
                backgroundColor: 'rgba(102, 126, 234, 0.2)',
                borderColor: '#667eea',
                borderWidth: 2,
                pointBackgroundColor: '#667eea',
                pointBorderColor: '#fff',
                pointRadius: 4,
                pointHoverRadius: 6,
                fill: true
            }, {
                label: '推荐标准 (100%)',
                data: [100, 100, 100, 100, 100, 100],
                backgroundColor: 'rgba(200, 200, 200, 0.1)',
                borderColor: '#94a3b8',
                borderWidth: 1.5,
                borderDash: [5, 5],
                pointBackgroundColor: '#94a3b8',
                pointRadius: 2,
                fill: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            scales: {
                r: {
                    beginAtZero: true,
                    max: 100,
                    ticks: {
                        stepSize: 20,
                        callback: function(value) {
                            return value + '%';
                        }
                    },
                    grid: {
                        color: '#e2e8f0'
                    },
                    pointLabels: {
                        font: {
                            size: 11
                        },
                        color: '#475569'
                    }
                }
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let label = context.dataset.label || '';
                            let value = context.raw;
                            return `${label}: ${value}%`;
                        }
                    }
                },
                legend: {
                    position: 'bottom',
                    labels: {
                        font: {
                            size: 11
                        },
                        boxWidth: 12,
                        usePointStyle: true
                    }
                }
            }
        }
    });
}


// 增强版建议生成函数
function generateAdviceTextEnhanced(avgScore, avgVegetables, avgFruit, avgProtein, avgFiber, breakfastCount, foodTypeCount) {
    const advices = [];

    if (avgScore < 60) {
        advices.push('📌 本周饮食评分偏低，建议减少高油高糖食物，增加蔬菜水果摄入。');
    } else if (avgScore < 80) {
        advices.push('📌 饮食基本健康，继续优化营养搭配会更好。');
    } else {
        advices.push('🎉 饮食评分优秀！继续保持良好的饮食习惯！');
    }

    if (avgVegetables < 300) {
        advices.push(`🥬 蔬菜摄入不足（${avgVegetables.toFixed(0)}g/天），建议每天至少吃300g绿叶蔬菜，深色蔬菜占一半。`);
    }
    if (avgFruit < 200) {
        advices.push(`🍎 水果摄入不足（${avgFruit.toFixed(0)}g/天），建议每天吃200-350g新鲜水果。`);
    }
    if (avgProtein < 60) {
        advices.push(`🥩 蛋白质摄入不足（${avgProtein.toFixed(0)}g/天），建议增加鸡蛋、鱼肉、豆腐等优质蛋白。`);
    }
    if (avgFiber < 25) {
        advices.push(`🌾 膳食纤维不足（${avgFiber.toFixed(0)}g/天），建议增加全谷物、豆类、菌菇类食物。`);
    }
    if (breakfastCount < 5) {
        advices.push(`🌅 早餐次数偏少（仅${breakfastCount}次），规律吃早餐有助于全天代谢。`);
    }
    if (foodTypeCount < 12) {
        advices.push(`🍽️ 食物多样性不足（仅${foodTypeCount}种/周），建议每周摄入25种以上不同食物。`);
    }

    if (advices.length === 0) {
        advices.push('👍 饮食结构良好，符合膳食指南建议，继续保持！');
    }

    return advices.join('<br>');
}

function generateAdviceText(avgScore, vegetableCount, breakfastCount, proteinCount) {
    const advices = [];

    if (avgScore < 60) {
        advices.push('📌 本周饮食评分偏低，建议减少高油高糖食物，增加蔬菜水果摄入。');
    } else if (avgScore < 80) {
        advices.push('📌 饮食基本健康，继续优化营养搭配会更好。');
    } else {
        advices.push('🎉 饮食评分优秀！继续保持良好的饮食习惯！');
    }

    if (vegetableCount < 7) {
        advices.push('🥬 蔬菜摄入不足（仅' + vegetableCount + '次），建议每天至少吃300g绿叶蔬菜。');
    }
    if (breakfastCount < 5) {
        advices.push('🌅 早餐次数偏少（仅' + breakfastCount + '次），规律吃早餐有助于全天代谢。');
    }
    if (proteinCount < 7) {
        advices.push('🥩 蛋白质摄入不足（仅' + proteinCount + '次），建议增加鸡蛋、鱼肉、豆腐等优质蛋白。');
    }

    if (advices.length === 0) {
        advices.push('👍 饮食结构良好，继续保持！');
    }

    return advices.join('<br>');
}

function initDietPanel() {
    updateMealButtonActive();
}

// 将函数挂载到 window
window.selectMeal = selectMeal;
window.addFoodItem = addFoodItem;
window.removeFoodItem = removeFoodItem;
window.updateFoodItem = updateFoodItem;
window.submitMealRecords = submitMealRecords;
window.loadWeekReport = loadWeekReport;
window.getMealName = getMealName;
window.editDietRecord = editDietRecord;
window.deleteDietRecord = deleteDietRecord;
window.cancelEdit = cancelEdit;