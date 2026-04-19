let allergiesList = [];
let avoidanceList = [];
let diseasesList = [];

// 渲染健康档案面板
function renderProfilePanel() {
    const panel = document.getElementById('profile-panel');
    panel.innerHTML = `
        <div class="form-card">
            <h4>📋 基本信息</h4>
            <div class="form-row"><label>年龄</label><input type="number" id="profileAge" placeholder="请输入年龄"></div>
            <div class="form-row">
                <label>性别</label>
                <select id="profileGender"><option value="">请选择</option><option value="MALE">男</option><option value="FEMALE">女</option></select>
            </div>
            <div class="form-row"><label>身高 (cm)</label><input type="number" id="profileHeight" step="0.1" placeholder="请输入身高"></div>
            <div class="form-row"><label>体重 (kg)</label><input type="number" id="profileWeight" step="0.1" placeholder="请输入体重"></div>
        </div>
        <div class="form-card">
            <h4>⚠️ 过敏史</h4>
            <div id="allergiesTags" class="tag-group"></div>
            <div class="tag-input"><input type="text" id="allergyInput" placeholder="输入过敏原" onkeypress="if(event.key==='Enter') addTag('allergies')"><button onclick="addTag('allergies')">添加</button></div>
        </div>
        <div class="form-card">
            <h4>🚫 忌口食物</h4>
            <div id="avoidanceTags" class="tag-group"></div>
            <div class="tag-input"><input type="text" id="avoidanceInput" placeholder="输入忌口食物" onkeypress="if(event.key==='Enter') addTag('avoidance')"><button onclick="addTag('avoidance')">添加</button></div>
        </div>
        <div class="form-card">
            <h4>📝 既往病史</h4>
            <div id="diseasesTags" class="tag-group"></div>
            <div class="tag-input"><input type="text" id="diseaseInput" placeholder="输入病史" onkeypress="if(event.key==='Enter') addTag('diseases')"><button onclick="addTag('diseases')">添加</button></div>
        </div>
        <button class="btn-primary" onclick="saveHealthProfile()">💾 保存档案</button>
    `;
}

async function loadHealthProfile() {
    if (!currentUser) return;
    try {
        const response = await fetch(`${API_BASE}/api/profile/${currentUser.userId}`);
        const data = await response.json();
        if (data.profile) {
            const profile = data.profile;
            document.getElementById('profileAge').value = profile.age || '';
            document.getElementById('profileGender').value = profile.gender || '';
            document.getElementById('profileHeight').value = profile.height || '';
            document.getElementById('profileWeight').value = profile.weight || '';
            allergiesList = data.allergies || [];
            avoidanceList = data.foodAvoidance || [];
            diseasesList = data.pastDiseases || [];
            renderTags();
        }
    } catch (error) {
        console.error('加载健康档案失败', error);
    }
}

function addTag(type) {
    let inputId, listName;
    if (type === 'allergies') { inputId = 'allergyInput'; listName = 'allergiesList'; }
    else if (type === 'avoidance') { inputId = 'avoidanceInput'; listName = 'avoidanceList'; }
    else { inputId = 'diseaseInput'; listName = 'diseasesList'; }

    const input = document.getElementById(inputId);
    const value = input.value.trim();
    if (value && !window[listName].includes(value)) {
        window[listName].push(value);
        renderTags();
        input.value = '';
    }
}

function removeTag(type, index) {
    let listName;
    if (type === 'allergies') listName = 'allergiesList';
    else if (type === 'avoidance') listName = 'avoidanceList';
    else listName = 'diseasesList';
    window[listName].splice(index, 1);
    renderTags();
}

function renderTags() {
    const allergiesDiv = document.getElementById('allergiesTags');
    if (allergiesDiv) allergiesDiv.innerHTML = allergiesList.map((item, i) => `<span class="tag selected">${escapeHtml(item)} <span style="cursor:pointer" onclick="removeTag('allergies', ${i})">×</span></span>`).join('');

    const avoidanceDiv = document.getElementById('avoidanceTags');
    if (avoidanceDiv) avoidanceDiv.innerHTML = avoidanceList.map((item, i) => `<span class="tag selected">${escapeHtml(item)} <span style="cursor:pointer" onclick="removeTag('avoidance', ${i})">×</span></span>`).join('');

    const diseasesDiv = document.getElementById('diseasesTags');
    if (diseasesDiv) diseasesDiv.innerHTML = diseasesList.map((item, i) => `<span class="tag selected">${escapeHtml(item)} <span style="cursor:pointer" onclick="removeTag('diseases', ${i})">×</span></span>`).join('');
}

async function saveHealthProfile() {
    if (!currentUser) return;
    const profile = {
        userId: currentUser.userId,
        age: parseInt(document.getElementById('profileAge').value) || null,
        gender: document.getElementById('profileGender').value || null,
        height: parseFloat(document.getElementById('profileHeight').value) || null,
        weight: parseFloat(document.getElementById('profileWeight').value) || null,
        allergies: JSON.stringify(allergiesList),
        foodAvoidance: JSON.stringify(avoidanceList),
        pastDiseases: JSON.stringify(diseasesList)
    };
    try {
        const response = await fetch(`${API_BASE}/api/profile/save`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(profile)
        });
        const data = await response.json();
        alert(data.success ? '健康档案保存成功！' : '保存失败');
    } catch (error) {
        alert('网络错误，请稍后再试');
    }
}