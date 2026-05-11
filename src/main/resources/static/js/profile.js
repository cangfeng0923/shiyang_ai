// profile.js - 健康档案管理（文档式展示 + 表单编辑）

let isEditing = false;
let currentProfile = null;

// 预设选项
const genderOptions = { MALE: '男', FEMALE: '女' };
const occupationTypes = { OFFICE: '办公室', STUDENT: '学生', MANUAL: '体力劳动', RETIRED: '退休' };
const tasteOptions = { SPICY: '🌶️ 喜辣', SWEET: '🍰 喜甜', LIGHT: '🥬 清淡', OILY_SALTY: '🍟 重油盐' };
const dietTypeOptions = { OMNIVORE: '🥩 杂食', VEGETARIAN: '🥬 全素', EGG_DAIRY_VEGAN: '🥚 蛋奶素', LOW_CARB: '🍚 低碳水', IF: '⏰ 间歇性断食' };
const exerciseFreqOptions = { NEVER: '从不', '1-2TIMES': '每周1-2次', '3-5TIMES': '每周3-5次', DAILY: '每天' };
const sleepQualityOptions = { GOOD: '😊 良好', FAIR: '😐 一般', POOR: '😔 较差', INSOMNIA: '😫 失眠' };

// ========== 全局标签数组（使用 window 挂载，确保全局唯一） ==========
window.allergiesList = [];
window.avoidanceList = [];
window.diseasesList = [];

// ========== 渲染健康档案面板 ==========
function renderProfilePanel() {
    const panel = document.getElementById('profile-panel');
    if (!panel) return;

    if (isEditing) {
        renderEditForm(panel);
    } else {
        loadAndRenderReport(panel);
    }
}

// ========== 加载并渲染报告 ==========
async function loadAndRenderReport(container) {
    if (!currentUser) return;

    container.innerHTML = '<div class="loading-spinner"></div> 加载中...';

    try {
        const response = await fetch(`${API_BASE}/api/profile/${currentUser.userId}`);
        const data = await response.json();
        currentProfile = data.profile;

        if (currentProfile) {
            renderProfileReport(container);
        } else {
            renderEmptyProfile(container);
        }
    } catch (error) {
        console.error('加载档案失败:', error);
        container.innerHTML = '<div class="empty-state">⚠️ 加载失败，请刷新重试</div>';
    }
}

// ========== 渲染空档案 ==========
function renderEmptyProfile(container) {
    container.innerHTML = `
        <div class="profile-report" style="text-align: center; padding: 40px;">
            <div style="font-size: 4rem; margin-bottom: 16px;">📋</div>
            <h3>还没有健康档案</h3>
            <p style="color: #666; margin: 8px 0 24px;">完善档案后，AI将为您提供更精准的养生建议</p>
            <button class="edit-btn" onclick="startEdit()">📝 立即填写</button>
        </div>
    `;
}

// ========== 渲染档案报告 ==========
function renderProfileReport(container) {
    const p = currentProfile;
    const age = p.age || (p.birthDate ? new Date().getFullYear() - new Date(p.birthDate).getFullYear() : '?');

    // 解析JSON数组
    const allergies = parseJsonArray(p.allergies);
    const avoidance = parseJsonArray(p.foodAvoidance);
    const diseases = parseJsonArray(p.pastDiseases);
    const medications = parseJsonArray(p.medications);
    const symptoms = parseJsonArray(p.symptoms);
    const cookingCondition = parseJsonArray(p.cookingCondition);

    // BMI状态
    let bmiStatus = '', bmiColor = '';
    if (p.bmi) {
        if (p.bmi < 18.5) { bmiStatus = '偏瘦'; bmiColor = '#f59e0b'; }
        else if (p.bmi < 24) { bmiStatus = '标准'; bmiColor = '#10b981'; }
        else if (p.bmi < 28) { bmiStatus = '超重'; bmiColor = '#f59e0b'; }
        else { bmiStatus = '肥胖'; bmiColor = '#ef4444'; }
    }

    container.innerHTML = `
        <div class="profile-report">
            <div class="profile-header">
                <h2>📋 健康档案</h2>
                <p>最后更新：${p.updateTime ? new Date(p.updateTime).toLocaleString() : '未知'}</p>
            </div>
            
            <!-- BMI卡片 -->
            <div class="bmi-card">
                <div><div class="bmi-value">${p.height || '?'}cm</div><div class="bmi-label">身高</div></div>
                <div><div class="bmi-value">${p.weight || '?'}kg</div><div class="bmi-label">体重</div></div>
                <div><div class="bmi-value" style="color: ${bmiColor}">${p.bmi || '?'}</div><div class="bmi-label">BMI · ${bmiStatus}</div></div>
                ${p.waistline ? `<div><div class="bmi-value">${p.waistline}cm</div><div class="bmi-label">腰围</div></div>` : ''}
            </div>
            
            <!-- 1. 基础信息 -->
            <div class="section-title">👤 基础信息</div>
            <div class="info-grid">
                <div class="info-item"><span class="info-label">昵称</span><span class="info-value">${p.nickname || currentUser.username || '未填写'}</span></div>
                <div class="info-item"><span class="info-label">性别</span><span class="info-value">${genderOptions[p.gender] || '未填写'}</span></div>
                <div class="info-item"><span class="info-label">年龄</span><span class="info-value">${age}岁</span></div>
                <div class="info-item"><span class="info-label">职业</span><span class="info-value">${p.occupation || '未填写'} ${occupationTypes[p.occupationType] ? '(' + occupationTypes[p.occupationType] + ')' : ''}</span></div>
                <div class="info-item"><span class="info-label">生理阶段</span><span class="info-value">${formatPhysiologicalStage(p.physiologicalStage)}</span></div>
            </div>
            
            <!-- 2. 健康状况 -->
            <div class="section-title">🏥 健康状况</div>
            <div class="info-grid">
                <div class="info-item"><span class="info-label">慢病史</span><div class="tag-list">${formatTagList(diseases)}</div></div>
                <div class="info-item"><span class="info-label">用药情况</span><div class="tag-list">${formatTagList(medications)}</div></div>
                ${renderHealthMetrics(p)}
            </div>
            
            <!-- 3. 饮食偏好 -->
            <div class="section-title">🍽️ 饮食偏好</div>
            <div class="info-grid">
                <div class="info-item"><span class="info-label">过敏原</span><div class="tag-list">${formatTagList(allergies, 'warning')}</div></div>
                <div class="info-item"><span class="info-label">忌口</span><div class="tag-list">${formatTagList(avoidance)}</div></div>
                <div class="info-item"><span class="info-label">口味偏好</span><span class="info-value">${tasteOptions[p.tastePreference] || '未填写'}</span></div>
                <div class="info-item"><span class="info-label">饮食类型</span><span class="info-value">${dietTypeOptions[p.dietType] || '未填写'}</span></div>
                <div class="info-item"><span class="info-label">用餐习惯</span><span class="info-value">${formatDiningHabit(p.diningHabit)}</span></div>
                <div class="info-item"><span class="info-label">烹饪条件</span><div class="tag-list">${formatTagList(cookingCondition)}</div></div>
            </div>
            
            <!-- 4. 中医特征 -->
            <div class="section-title">🌿 中医特征</div>
            <div class="info-grid">
                <div class="info-item"><span class="info-label">体质类型</span><span class="info-value">${p.constitution || '未测评'}</span></div>
                <div class="info-item"><span class="info-label">常见症状</span><div class="tag-list">${formatTagList(symptoms)}</div></div>
                <div class="info-item"><span class="info-label">舌象描述</span><span class="info-value">${p.tongueDescription || '未填写'}</span></div>
            </div>
            
            <!-- 5. 生活方式 -->
            <div class="section-title">🏃 生活方式</div>
            <div class="info-grid">
                <div class="info-item"><span class="info-label">运动频率</span><span class="info-value">${exerciseFreqOptions[p.exerciseFrequency] || '未填写'}</span></div>
                <div class="info-item"><span class="info-label">睡眠时长</span><span class="info-value">${p.sleepDuration ? p.sleepDuration + '小时/天' : '未填写'}</span></div>
                <div class="info-item"><span class="info-label">睡眠质量</span><span class="info-value">${sleepQualityOptions[p.sleepQuality] || '未填写'}</span></div>
                <div class="info-item"><span class="info-label">饮水</span><span class="info-value">${p.waterIntake ? p.waterIntake + 'ml/天' : '未填写'}</span></div>
                <div class="info-item"><span class="info-label">吸烟/饮酒</span><span class="info-value">${formatSmokingDrinking(p.smoking, p.drinking)}</span></div>
            </div>
            
            <button class="edit-btn" onclick="startEdit()">✏️ 修改档案</button>
        </div>
    `;
}

// ========== 渲染生化指标 ==========
function renderHealthMetrics(p) {
    let html = '';
    if (p.systolicBp && p.diastolicBp) {
        html += `<div class="info-item"><span class="info-label">血压</span><span class="info-value">${p.systolicBp}/${p.diastolicBp} mmHg</span></div>`;
    }
    if (p.fastingGlucose) {
        html += `<div class="info-item"><span class="info-label">空腹血糖</span><span class="info-value">${p.fastingGlucose} mmol/L</span></div>`;
    }
    if (p.totalCholesterol) {
        html += `<div class="info-item"><span class="info-label">总胆固醇</span><span class="info-value">${p.totalCholesterol} mmol/L</span></div>`;
    }
    if (p.uricAcid) {
        html += `<div class="info-item"><span class="info-label">尿酸</span><span class="info-value">${p.uricAcid} μmol/L</span></div>`;
    }
    return html;
}

// ========== 开始编辑 ==========
function startEdit() {
    isEditing = true;
    renderProfilePanel();
}

// ========== 取消编辑 ==========
function cancelEdit() {
    isEditing = false;
    renderProfilePanel();
}

// ========== 渲染编辑表单 ==========
function renderEditForm(container) {
    const p = currentProfile || {};

    // ✅ 从档案加载已有数据到全局变量
    window.allergiesList = parseJsonArray(p.allergies);
    window.avoidanceList = parseJsonArray(p.foodAvoidance);
    window.diseasesList = parseJsonArray(p.pastDiseases);

    console.log('=== 编辑表单加载 ===');
    console.log('过敏源:', window.allergiesList);
    console.log('忌口:', window.avoidanceList);

    container.innerHTML = `
        <div class="edit-form">
            <h3 style="margin:0 0 20px; text-align:center;">📝 完善健康档案</h3>
            
            <!-- 基础信息 -->
            <div class="form-section">
                <h4>👤 基础信息</h4>
                <div class="form-row-group">
                    <div class="form-group"><label>昵称</label><input type="text" id="nickname" value="${escapeHtml(p.nickname || '')}" placeholder="如何称呼您"></div>
                    <div class="form-group"><label>性别</label><select id="gender"><option value="">请选择</option><option value="MALE" ${p.gender === 'MALE' ? 'selected' : ''}>男</option><option value="FEMALE" ${p.gender === 'FEMALE' ? 'selected' : ''}>女</option></select></div>
                    <div class="form-group"><label>出生日期</label><input type="date" id="birthDate" value="${p.birthDate || ''}"></div>
                    <div class="form-group"><label>职业</label><input type="text" id="occupation" value="${escapeHtml(p.occupation || '')}" placeholder="例如：程序员"></div>
                    <div class="form-group"><label>职业类型</label><select id="occupationType"><option value="">请选择</option><option value="OFFICE" ${p.occupationType === 'OFFICE' ? 'selected' : ''}>办公室久坐</option><option value="STUDENT" ${p.occupationType === 'STUDENT' ? 'selected' : ''}>学生</option><option value="MANUAL" ${p.occupationType === 'MANUAL' ? 'selected' : ''}>体力劳动</option><option value="RETIRED" ${p.occupationType === 'RETIRED' ? 'selected' : ''}>退休</option></select></div>
                    <div class="form-group"><label>生理阶段（女性）</label><select id="physiologicalStage"><option value="">请选择</option><option value="NORMAL" ${p.physiologicalStage === 'NORMAL' ? 'selected' : ''}>正常</option><option value="PREGNANT" ${p.physiologicalStage === 'PREGNANT' ? 'selected' : ''}>孕期</option><option value="LACTATING" ${p.physiologicalStage === 'LACTATING' ? 'selected' : ''}>哺乳期</option><option value="MENOPAUSE" ${p.physiologicalStage === 'MENOPAUSE' ? 'selected' : ''}>更年期</option></select></div>
                </div>
            </div>
            
            <!-- 身体指标 -->
            <div class="form-section">
                <h4>📏 身体指标</h4>
                <div class="form-row-group">
                    <div class="form-group"><label>身高(cm)</label><input type="number" id="height" step="0.1" value="${p.height || ''}"></div>
                    <div class="form-group"><label>体重(kg)</label><input type="number" id="weight" step="0.1" value="${p.weight || ''}"></div>
                    <div class="form-group"><label>腰围(cm)</label><input type="number" id="waistline" step="0.1" value="${p.waistline || ''}"></div>
                </div>
            </div>
            
            <!-- 健康指标 -->
            <div class="form-section">
                <h4>🏥 健康指标（选填）</h4>
                <div class="form-row-group">
                    <div class="form-group"><label>收缩压/舒张压</label><div style="display:flex; gap:8px;"><input type="number" id="systolicBp" placeholder="收缩压" value="${p.systolicBp || ''}" style="flex:1"><span style="line-height:34px;">/</span><input type="number" id="diastolicBp" placeholder="舒张压" value="${p.diastolicBp || ''}" style="flex:1"></div></div>
                    <div class="form-group"><label>空腹血糖(mmol/L)</label><input type="number" id="fastingGlucose" step="0.1" value="${p.fastingGlucose || ''}"></div>
                    <div class="form-group"><label>总胆固醇(mmol/L)</label><input type="number" id="totalCholesterol" step="0.1" value="${p.totalCholesterol || ''}"></div>
                    <div class="form-group"><label>尿酸(μmol/L)</label><input type="number" id="uricAcid" value="${p.uricAcid || ''}"></div>
                </div>
            </div>
            
            <!-- 饮食偏好 -->
            <div class="form-section">
                <h4>🍽️ 饮食偏好</h4>
                <div class="form-row-group">
                    <div class="form-group"><label>过敏原</label><div class="tag-input-group"><input type="text" id="allergyInput" placeholder="如：花生、海鲜"><button type="button" onclick="window.addTagField('allergies')">+</button></div><div id="allergiesTags" class="tag-list"></div></div>
                    <div class="form-group"><label>忌口食物</label><div class="tag-input-group"><input type="text" id="avoidanceInput" placeholder="如：香菜、内脏"><button type="button" onclick="window.addTagField('avoidance')">+</button></div><div id="avoidanceTags" class="tag-list"></div></div>
                    <div class="form-group"><label>口味偏好</label><select id="tastePreference"><option value="">请选择</option><option value="SPICY" ${p.tastePreference === 'SPICY' ? 'selected' : ''}>🌶️ 喜辣</option><option value="SWEET" ${p.tastePreference === 'SWEET' ? 'selected' : ''}>🍰 喜甜</option><option value="LIGHT" ${p.tastePreference === 'LIGHT' ? 'selected' : ''}>🥬 清淡</option><option value="OILY_SALTY" ${p.tastePreference === 'OILY_SALTY' ? 'selected' : ''}>🍟 重油盐</option></select></div>
                    <div class="form-group"><label>饮食类型</label><select id="dietType"><option value="">请选择</option><option value="OMNIVORE" ${p.dietType === 'OMNIVORE' ? 'selected' : ''}>杂食</option><option value="VEGETARIAN" ${p.dietType === 'VEGETARIAN' ? 'selected' : ''}>全素</option><option value="EGG_DAIRY_VEGAN" ${p.dietType === 'EGG_DAIRY_VEGAN' ? 'selected' : ''}>蛋奶素</option><option value="LOW_CARB" ${p.dietType === 'LOW_CARB' ? 'selected' : ''}>低碳水</option><option value="IF" ${p.dietType === 'IF' ? 'selected' : ''}>间歇性断食</option></select></div>
                </div>
            </div>
            
            <!-- 中医特征 -->
            <div class="form-section">
                <h4>🌿 中医特征</h4>
                <div class="form-row-group">
                    <div class="form-group"><label>体质类型</label><select id="constitution"><option value="">请选择</option><option value="平和质" ${p.constitution === '平和质' ? 'selected' : ''}>平和质</option><option value="气虚质" ${p.constitution === '气虚质' ? 'selected' : ''}>气虚质</option><option value="阳虚质" ${p.constitution === '阳虚质' ? 'selected' : ''}>阳虚质</option><option value="阴虚质" ${p.constitution === '阴虚质' ? 'selected' : ''}>阴虚质</option><option value="痰湿质" ${p.constitution === '痰湿质' ? 'selected' : ''}>痰湿质</option><option value="湿热质" ${p.constitution === '湿热质' ? 'selected' : ''}>湿热质</option><option value="血瘀质" ${p.constitution === '血瘀质' ? 'selected' : ''}>血瘀质</option><option value="气郁质" ${p.constitution === '气郁质' ? 'selected' : ''}>气郁质</option></select></div>
                </div>
            </div>
            
            <!-- 生活方式 -->
            <div class="form-section">
                <h4>🏃 生活方式</h4>
                <div class="form-row-group">
                    <div class="form-group"><label>运动频率</label><select id="exerciseFrequency"><option value="">请选择</option><option value="NEVER" ${p.exerciseFrequency === 'NEVER' ? 'selected' : ''}>从不</option><option value="1-2TIMES" ${p.exerciseFrequency === '1-2TIMES' ? 'selected' : ''}>每周1-2次</option><option value="3-5TIMES" ${p.exerciseFrequency === '3-5TIMES' ? 'selected' : ''}>每周3-5次</option><option value="DAILY" ${p.exerciseFrequency === 'DAILY' ? 'selected' : ''}>每天</option></select></div>
                    <div class="form-group"><label>平均睡眠时长(小时)</label><input type="number" id="sleepDuration" step="0.5" value="${p.sleepDuration || ''}" placeholder="例如：7.5"></div>
                    <div class="form-group"><label>睡眠质量</label><select id="sleepQuality"><option value="">请选择</option><option value="GOOD" ${p.sleepQuality === 'GOOD' ? 'selected' : ''}>良好</option><option value="FAIR" ${p.sleepQuality === 'FAIR' ? 'selected' : ''}>一般</option><option value="POOR" ${p.sleepQuality === 'POOR' ? 'selected' : ''}>较差</option><option value="INSOMNIA" ${p.sleepQuality === 'INSOMNIA' ? 'selected' : ''}>失眠</option></select></div>
                    <div class="form-group"><label>每日饮水量(ml)</label><input type="number" id="waterIntake" value="${p.waterIntake || ''}" placeholder="例如：1500"></div>
                </div>
            </div>
            
            <div class="action-buttons">
                <button class="save-btn" onclick="window.saveProfileEdit()">💾 保存档案</button>
                <button class="cancel-btn" onclick="window.cancelEdit()">取消</button>
            </div>
        </div>
    `;

    // 渲染标签
    renderTagFields();
}

// ========== 渲染标签字段 ==========
function renderTagFields() {
    const allergiesDiv = document.getElementById('allergiesTags');
    if (allergiesDiv) {
        allergiesDiv.innerHTML = (window.allergiesList || []).map((item, i) =>
            `<span class="tag">${escapeHtml(item)} <span style="cursor:pointer" onclick="window.removeTagItem('allergies', ${i})">×</span></span>`
        ).join('');
    }

    const avoidanceDiv = document.getElementById('avoidanceTags');
    if (avoidanceDiv) {
        avoidanceDiv.innerHTML = (window.avoidanceList || []).map((item, i) =>
            `<span class="tag">${escapeHtml(item)} <span style="cursor:pointer" onclick="window.removeTagItem('avoidance', ${i})">×</span></span>`
        ).join('');
    }
}

// ========== 添加标签 ==========
function addTagField(type) {
    let inputId, list;
    if (type === 'allergies') {
        inputId = 'allergyInput';
        list = window.allergiesList;
    } else if (type === 'avoidance') {
        inputId = 'avoidanceInput';
        list = window.avoidanceList;
    } else {
        return;
    }

    const input = document.getElementById(inputId);
    const value = input.value.trim();

    console.log(`添加 ${type}:`, value);

    if (value && !list.includes(value)) {
        list.push(value);
        console.log(`${type} 列表现在:`, list);
        renderTagFields();
        input.value = '';
    }
}

// ========== 删除标签 ==========
function removeTagItem(type, index) {
    if (type === 'allergies') {
        window.allergiesList.splice(index, 1);
        console.log('删除后过敏源列表:', window.allergiesList);
    } else if (type === 'avoidance') {
        window.avoidanceList.splice(index, 1);
        console.log('删除后忌口列表:', window.avoidanceList);
    }
    renderTagFields();
}

// ========== 保存编辑 ==========
async function saveProfileEdit() {
    if (!currentUser) return;

    console.log('保存前 - 过敏源列表:', window.allergiesList);
    console.log('保存前 - 忌口列表:', window.avoidanceList);

    const profile = {
        userId: currentUser.userId,
        nickname: document.getElementById('nickname')?.value || null,
        gender: document.getElementById('gender')?.value || null,
        birthDate: document.getElementById('birthDate')?.value || null,
        occupation: document.getElementById('occupation')?.value || null,
        occupationType: document.getElementById('occupationType')?.value || null,
        physiologicalStage: document.getElementById('physiologicalStage')?.value || null,
        height: parseFloat(document.getElementById('height')?.value) || null,
        weight: parseFloat(document.getElementById('weight')?.value) || null,
        waistline: parseFloat(document.getElementById('waistline')?.value) || null,
        systolicBp: parseInt(document.getElementById('systolicBp')?.value) || null,
        diastolicBp: parseInt(document.getElementById('diastolicBp')?.value) || null,
        fastingGlucose: parseFloat(document.getElementById('fastingGlucose')?.value) || null,
        totalCholesterol: parseFloat(document.getElementById('totalCholesterol')?.value) || null,
        uricAcid: parseInt(document.getElementById('uricAcid')?.value) || null,
        tastePreference: document.getElementById('tastePreference')?.value || null,
        dietType: document.getElementById('dietType')?.value || null,
        constitution: document.getElementById('constitution')?.value || null,
        exerciseFrequency: document.getElementById('exerciseFrequency')?.value || null,
        sleepDuration: parseFloat(document.getElementById('sleepDuration')?.value) || null,
        sleepQuality: document.getElementById('sleepQuality')?.value || null,
        waterIntake: parseInt(document.getElementById('waterIntake')?.value) || null,
        allergies: JSON.stringify(window.allergiesList || []),
        foodAvoidance: JSON.stringify(window.avoidanceList || []),
        pastDiseases: JSON.stringify(window.diseasesList || [])
    };

    try {
        const response = await fetch(`${API_BASE}/api/profile/save`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(profile)
        });
        const data = await response.json();
        if (data.success) {
            alert('健康档案保存成功！');
            isEditing = false;
            // 重新加载档案
            const panel = document.getElementById('profile-panel');
            if (panel) {
                await loadAndRenderReport(panel);
            }
        } else {
            alert('保存失败：' + (data.message || '未知错误'));
        }
    } catch (error) {
        console.error('保存失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// ========== 辅助函数 ==========
function parseJsonArray(str) {
    if (!str) return [];
    try { return JSON.parse(str); } catch(e) { return []; }
}

function formatTagList(tags, type = 'normal') {
    if (!tags || tags.length === 0) return '<span class="empty-tag">未填写</span>';
    return tags.map(t => `<span class="tag">${escapeHtml(t)}</span>`).join('');
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}

function formatPhysiologicalStage(stage) {
    const map = { NORMAL: '正常', PREGNANT: '🤰 孕期', LACTATING: '🍼 哺乳期', MENOPAUSE: '🌺 更年期' };
    return map[stage] || '未填写';
}

function formatDiningHabit(habit) {
    const map = { HOME_COOKING: '🏠 自己做饭', TAKEOUT: '📱 外卖为主', BOTH: '🍽️ 两者结合' };
    return map[habit] || '未填写';
}

function formatSmokingDrinking(smoking, drinking) {
    const smokeMap = { NEVER: '不吸烟', OCCASIONAL: '偶尔', DAILY: '每天' };
    const drinkMap = { NEVER: '不饮酒', OCCASIONAL: '偶尔', SOCIAL: '应酬饮酒', DAILY: '每天' };
    return `${smokeMap[smoking] || '未填写'} · ${drinkMap[drinking] || '未填写'}`;
}

// ========== 挂载全局函数 ==========
window.renderProfilePanel = renderProfilePanel;
window.startEdit = startEdit;
window.cancelEdit = cancelEdit;
window.saveProfileEdit = saveProfileEdit;
window.addTagField = addTagField;
window.removeTagItem = removeTagItem;