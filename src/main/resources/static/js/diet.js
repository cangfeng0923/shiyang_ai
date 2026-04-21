let currentMeal = 'BREAKFAST';

function renderDietPanel() {
    const panel = document.getElementById('diet-panel');
    if (!panel) return;

    panel.innerHTML = `
        <div class="form-card">
            <h4>🍽️ 记录今日饮食</h4>
            <div class="meal-buttons">
                <button class="meal-btn" data-meal="BREAKFAST" onclick="selectMeal('BREAKFAST')">🌅 早餐</button>
                <button class="meal-btn" data-meal="LUNCH" onclick="selectMeal('LUNCH')">☀️ 午餐</button>
                <button class="meal-btn" data-meal="DINNER" onclick="selectMeal('DINNER')">🌙 晚餐</button>
                <button class="meal-btn" data-meal="SNACK" onclick="selectMeal('SNACK')">🍪 加餐</button>
            </div>
            <div class="form-row"><input type="text" id="foodName" placeholder="食物名称" style="margin-bottom: 12px;"></div>
            <div class="form-row"><input type="number" id="foodGrams" placeholder="重量 (克)" style="margin-bottom: 12px;"></div>
            <div class="form-row"><textarea id="foodNotes" rows="2" placeholder="备注（可选）" style="width:100%; padding:10px; border-radius:10px; border:1px solid #e0e4e8; resize:none;"></textarea></div>
            <button class="btn-primary" onclick="addDietRecord()">📝 记录</button>
        </div>
        <div class="form-card"><h4>📅 今日饮食</h4><div id="todayRecords" class="record-list"></div></div>
    `;

    // 高亮当前选中的餐次按钮
    updateMealButtonActive();
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

async function addDietRecord() {
    if (!currentUser) {
        alert('请先登录');
        return;
    }

    const foodName = document.getElementById('foodName').value.trim();
    const grams = parseInt(document.getElementById('foodGrams').value);
    const notes = document.getElementById('foodNotes').value;

    if (!foodName) {
        alert('请输入食物名称');
        return;
    }

    // ✅ 修改：发送完整的日期时间格式（带时间）
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');

    // 格式: "2026-04-21T22:30:00"
    const recordDateTime = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`;

    const record = {
        userId: currentUser.userId,
        mealType: currentMeal,
        foodName: foodName,
        grams: grams || null,
        notes: notes || null,
        recordDate: recordDateTime  // ✅ 发送完整时间格式
    };

    try {
        const response = await fetch(`${API_BASE}/api/diet/record`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(record)
        });
        const data = await response.json();

        if (data.success) {
            alert(`记录成功！健康评分：${data.healthScore}/100\n${data.suggestions}`);
            // 清空表单
            document.getElementById('foodName').value = '';
            document.getElementById('foodGrams').value = '';
            document.getElementById('foodNotes').value = '';
            // 刷新今日记录
            await loadTodayRecords();
        } else {
            alert('记录失败: ' + (data.message || '未知错误'));
        }
    } catch (error) {
        console.error('记录失败:', error);
        alert('网络错误: ' + error.message);
    }
}

async function loadTodayRecords() {
    if (!currentUser) return;

    const container = document.getElementById('todayRecords');
    if (!container) return;

    try {
        const response = await fetch(`${API_BASE}/api/diet/today/${currentUser.userId}`);
        const data = await response.json();

        if (data.records && data.records.length > 0) {
            container.innerHTML = data.records.map(record => {
                let scoreClass = 'score-good';
                if (record.healthScore >= 80) scoreClass = 'score-excellent';
                if (record.healthScore < 60) scoreClass = 'score-poor';

                // 格式化时间显示
                let timeDisplay = '';
                if (record.recordDate) {
                    const date = new Date(record.recordDate);
                    timeDisplay = ` ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
                }

                return `<div class="record-item">
                    <div style="flex:1">
                        <strong>${getMealName(record.mealType)}${timeDisplay}</strong><br>
                        ${escapeHtml(record.foodName)} ${record.grams ? record.grams + 'g' : ''}
                        ${record.notes ? '<br><small style="color:#999;">📝 ' + escapeHtml(record.notes) + '</small>' : ''}
                    </div>
                    <div class="${scoreClass}" style="font-weight:bold; min-width:40px; text-align:center;">
                        ${record.healthScore}
                    </div>
                </div>`;
            }).join('');
        } else {
            container.innerHTML = '<p style="color:#999; text-align:center; padding:20px;">今日暂无饮食记录</p>';
        }
    } catch (error) {
        console.error('加载饮食记录失败:', error);
        container.innerHTML = '<p style="color:#999; text-align:center;">加载失败</p>';
    }
}

// 初始化时选中早餐按钮
function initDietPanel() {
    updateMealButtonActive();
}