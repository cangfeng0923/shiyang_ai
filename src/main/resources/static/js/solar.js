function renderSolarPanel() {
    const panel = document.getElementById('solar-panel');
    panel.innerHTML = `<div class="solar-card" id="solarContent">加载中...</div>`;
}

async function loadSolarInfo() {
    try {
        const response = await fetch(`${API_BASE}/api/solar-term/info`);
        const data = await response.json();
        document.getElementById('solarContent').innerHTML = `
            <h3>📅 ${data.name}</h3>
            <p><strong>🌿 养生原则：</strong>${data.principle}</p>
            <p><strong>🍽️ 饮食建议：</strong>${data.foodAdvice}</p>
            <p><strong>🍲 推荐食谱：</strong>${data.recipe}</p>
            <p><strong>💤 起居建议：</strong>${data.dailyAdvice}</p>
        `;
    } catch (error) {
        document.getElementById('solarContent').innerHTML = '<p>加载失败</p>';
    }
}