const questions = [
    "经常感觉疲劳、气短？", "怕冷、手脚冰凉？", "口干舌燥、手脚心热？",
    "身体沉重、容易犯困？", "脸上容易出油、长痘？", "面色晦暗、容易长斑？",
    "情绪低落、容易焦虑？", "容易过敏（打喷嚏、皮疹）？", "怕风、容易感冒？", "腰膝酸软、记忆力下降？"
];

function renderAssessmentPanel() {
    const panel = document.getElementById('assessment-panel');
    let html = '<div class="form-card"><h4>📝 中医体质测评</h4><div id="questionsList">';
    questions.forEach((q, i) => {
        html += `<div class="question-item" style="display:flex; align-items:center; gap:10px; padding:10px 0; border-bottom:1px solid #eee;">
            <input type="checkbox" id="q${i}" style="width:18px; height:18px;">
            <label for="q${i}" style="flex:1; cursor:pointer;">${i+1}. ${q}</label>
        </div>`;
    });
    html += '</div><button class="btn-primary" onclick="submitAssessment()" style="margin-top:20px;">开始测评</button></div>';
    panel.innerHTML = html;
}

async function submitAssessment() {
    if (!currentUser) return;
    let answers = [];
    for (let i = 0; i < questions.length; i++) answers.push(document.getElementById(`q${i}`).checked);
    try {
        const response = await fetch(`${API_BASE}/api/assessment`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.userId, answers: answers })
        });
        const data = await response.json();
        if (data.constitution) {
            currentUser.constitution = data.constitution;
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            document.getElementById('constitutionDisplay').innerText = data.constitution;
            document.getElementById('avatarEmoji').innerText = getAvatarByConstitution(data.constitution);
            alert(`测评完成！您的体质是：${data.constitution}`);
            addAIMessage(`经过测评，您是${data.constitution}体质。${getConstitutionAdvice(data.constitution)}`);
            switchPanel('chat');
        } else alert('测评失败');
    } catch (error) { alert('网络错误'); }
}