let currentMeal = 'BREAKFAST';

function renderDietPanel() {
    const panel = document.getElementById('diet-panel');
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
}

function selectMeal(meal) {
    currentMeal = meal;
    document.querySelectorAll('.meal-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelector(`.meal-btn[data-meal="${meal}"]`).classList.add('active');
}

async function addDietRecord() {
    if (!currentUser) return;
    const foodName = document.getElementById('foodName').value.trim();
    const grams = parseInt(document.getElementById('foodGrams').value);
    const notes = document.getElementById('foodNotes').value;
    if (!foodName) { alert('请输入食物名称'); return; }

    const record = {
        userId: currentUser.userId, mealType: currentMeal, foodName: foodName,
        grams: grams || null, notes: notes || null, recordDate: new Date().toISOString().split('T')[0]
    };
    try {
        const response = await fetch(`${API_BASE}/api/diet/record`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(record)
        });
        const data = await response.json();
        if (data.success) {
            alert(`记录成功！健康评分：${data.healthScore}/100\n${data.suggestions}`);
            document.getElementById('foodName').value = '';
            document.getElementById('foodGrams').value = '';
            document.getElementById('foodNotes').value = '';
            loadTodayRecords();
        } else alert('记录失败');
    } catch (error) { alert('网络错误'); }
}

async function loadTodayRecords() {
    if (!currentUser) return;
    try {
        const response = await fetch(`${API_BASE}/api/diet/today/${currentUser.userId}`);
        const data = await response.json();
        const container = document.getElementById('todayRecords');
        if (data.records && data.records.length > 0) {
            container.innerHTML = data.records.map(record => {
                let scoreClass = 'score-good';
                if (record.healthScore >= 80) scoreClass = 'score-excellent';
                if (record.healthScore < 60) scoreClass = 'score-poor';
                return `<div class="record-item"><div><strong>${getMealName(record.mealType)}</strong><br>${record.foodName} ${record.grams ? record.grams + 'g' : ''}</div><div class="${scoreClass}" style="font-weight:bold">${record.healthScore}</div></div>`;
            }).join('');
        } else container.innerHTML = '<p style="color:#999; text-align:center;">今日暂无饮食记录</p>';
    } catch (error) { console.error(error); }
}