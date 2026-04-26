// solar.js - AI动态生成节气建议

let currentCity = '北京';
let recentCities = [];

// 加载历史城市
function loadRecentCities() {
    const saved = localStorage.getItem('recentCities');
    if (saved) {
        recentCities = JSON.parse(saved);
    }
    if (currentCity && !recentCities.includes(currentCity)) {
        recentCities.unshift(currentCity);
        if (recentCities.length > 5) recentCities.pop();
        saveRecentCities();
    }
}

function saveRecentCities() {
    localStorage.setItem('recentCities', JSON.stringify(recentCities));
}

async function renderSolarPanel() {
    const panel = document.getElementById('solar-panel');
    if (!panel) return;

    loadRecentCities();

    panel.innerHTML = `
        <div class="form-card">
            <!-- 城市下拉菜单 -->
            <div id="cityDropdown" style="display: none; position: absolute; background: white; border-radius: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.15); width: 250px; z-index: 100; overflow: hidden;">
                <div style="padding: 12px; border-bottom: 1px solid #e2e8f0;">
                    <div style="font-size: 0.7rem; color: #94a3b8; margin-bottom: 8px;">最近使用</div>
                    <div id="recentCitiesList"></div>
                </div>
                <div style="padding: 12px; display: flex; gap: 8px;">
                    <input type="text" id="newCityInput" placeholder="输入城市名" style="flex:1; padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 20px;" onkeypress="if(event.key==='Enter') addNewCity()">
                    <button onclick="addNewCity()" style="background: #667eea; color: white; border: none; padding: 8px 16px; border-radius: 20px;">+ 添加</button>
                </div>
            </div>
            
            <!-- 内容容器 -->
            <div id="solarContent" style="min-height: 400px;">
                <div class="loading-spinner" style="margin: 40px auto;"></div>
            </div>
        </div>
    `;

    renderRecentCitiesList();
    await loadAllData();
}

function renderRecentCitiesList() {
    const container = document.getElementById('recentCitiesList');
    if (!container) return;

    if (recentCities.length === 0) {
        container.innerHTML = '<div style="font-size: 0.8rem; color: #94a3b8; text-align: center; padding: 8px;">暂无历史城市</div>';
        return;
    }

    // ✅ 修复：使用 onclick 调用 selectCity，确保函数存在
    container.innerHTML = recentCities.map(city => `
        <div onclick="window.selectCity('${city.replace(/'/g, "\\'")}')" style="padding: 8px 12px; cursor: pointer; display: flex; align-items: center; gap: 8px; border-radius: 8px;">
            <span>📍</span> ${city}
        </div>
    `).join('');
}

function toggleCityDropdown() {
    const dropdown = document.getElementById('cityDropdown');
    if (dropdown) {
        dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
    }
}

function selectCity(city) {
    console.log('切换城市:', city);
    currentCity = city;
    addRecentCity(city);
    const dropdown = document.getElementById('cityDropdown');
    if (dropdown) dropdown.style.display = 'none';
    renderRecentCitiesList();

    // 更新顶部显示
    const citySpan = document.querySelector('.current-city span');
    if (citySpan) {
        citySpan.innerHTML = `📍 ${currentCity}`;
    }

    loadAllData();
}

function addNewCity() {
    const input = document.getElementById('newCityInput');
    const newCity = input.value.trim();
    if (newCity) {
        selectCity(newCity);
        input.value = '';
    }
}

function addRecentCity(city) {
    recentCities = recentCities.filter(c => c !== city);
    recentCities.unshift(city);
    if (recentCities.length > 5) recentCities.pop();
    saveRecentCities();
}

// 点击其他地方关闭下拉菜单
document.addEventListener('click', function(event) {
    const dropdown = document.getElementById('cityDropdown');
    const citySelector = document.querySelector('.current-city');
    if (dropdown && citySelector && !citySelector.contains(event.target) && !dropdown.contains(event.target)) {
        dropdown.style.display = 'none';
    }
});

async function loadAllData() {
    await Promise.all([
        loadWeatherInfo(),
        loadAdvice()
    ]);
}

async function loadWeatherInfo() {
    try {
        const response = await fetch(`${API_BASE}/api/solar-term/weather?city=${encodeURIComponent(currentCity)}`);
        const data = await response.json();

        if (data.success) {
            window.weatherData = data;
        }
    } catch (error) {
        console.error('加载天气失败:', error);
        window.weatherData = null;
    }
}

async function loadAdvice() {
    if (!currentUser) return;

    const container = document.getElementById('solarContent');
    if (!container) return;

    container.innerHTML = '<div class="loading-spinner" style="margin: 40px auto;"></div>';

    try {
        const constitution = currentUser.constitution || '平和质';
        const response = await fetch(`${API_BASE}/api/solar-term/ai-advice?userId=${currentUser.userId}&constitution=${encodeURIComponent(constitution)}&city=${encodeURIComponent(currentCity)}`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();

        if (data.success && data.advice) {
            displayContent(container, data.advice);
        } else {
            throw new Error('返回数据无效');
        }

    } catch (error) {
        console.error('加载节气建议失败:', error);
        displayFallbackContent(container);
    }
}

function displayContent(container, advice) {
    // 获取天气图标
    const getWeatherIcon = (weather) => {
        const iconMap = {
            '晴': '☀️', '多云': '⛅', '阴': '☁️', '小雨': '🌧️', '中雨': '🌧️', '大雨': '🌧️',
            '雷阵雨': '⛈️', '雪': '❄️', '小雪': '❄️', '中雪': '❄️', '大雪': '❄️',
            '雾': '🌫️', '霾': '😷'
        };
        return iconMap[weather] || '🌡️';
    };

    // 构建天气卡片（包含城市选择）
    let weatherHtml = '';
    if (window.weatherData && window.weatherData.success) {
        weatherHtml = `
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 20px; padding: 20px; margin-bottom: 20px; color: white;">
                <!-- 可点击的城市区域，点击切换城市 -->
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                    <div style="cursor: pointer;" onclick="toggleCityDropdown()">
                        <div style="display: flex; align-items: center; gap: 6px;">
                            <span style="font-size: 1.2rem;">📍</span>
                            <span style="font-size: 1.2rem; font-weight: 500;">${window.weatherData.city}</span>
                            <span style="font-size: 0.7rem;">▼</span>
                        </div>
                        <div style="font-size: 2rem; font-weight: bold; margin-top: 5px;">${window.weatherData.today?.temp || '--'}</div>
                        <div style="font-size: 0.9rem; margin-top: 5px;">${window.weatherData.today?.weather || '--'} · ${window.weatherData.today?.wind || '--'}</div>
                    </div>
                    <div style="font-size: 3.5rem;">${getWeatherIcon(window.weatherData.today?.weather)}</div>
                </div>
                <!-- 刷新按钮放在右上角小图标 -->
                <div style="text-align: right; margin-top: -40px; margin-bottom: 10px;">
                    <button onclick="refreshAdvice()" style="background: rgba(255,255,255,0.2); border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; font-size: 0.9rem; color: white;">🔄</button>
                </div>
                <!-- 3天预报 -->
                <div style="display: flex; justify-content: space-around; margin-top: 15px; padding-top: 15px; border-top: 1px solid rgba(255,255,255,0.2);">
        `;

        if (window.weatherData.daily && window.weatherData.daily.length > 0) {
            const weekdays = ['今天', '明天', '后天'];
            window.weatherData.daily.forEach((day, index) => {
                weatherHtml += `
                    <div style="text-align: center; flex: 1;">
                        <div style="font-size: 0.8rem; opacity: 0.8;">${weekdays[index]}</div>
                        <div style="font-size: 1.5rem; margin: 8px 0;">${getWeatherIcon(day.dayWeather)}</div>
                        <div style="font-size: 0.9rem; font-weight: bold;">${day.dayTemp}</div>
                        <div style="font-size: 0.7rem; opacity: 0.7;">${day.dayWeather}</div>
                    </div>
                `;
            });
        }

        weatherHtml += `
                </div>
            </div>
        `;
    } else {
        weatherHtml = `
            <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 20px; padding: 20px; margin-bottom: 20px; color: white;">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div style="cursor: pointer;" onclick="toggleCityDropdown()">
                        <div style="display: flex; align-items: center; gap: 6px;">
                            <span style="font-size: 1.2rem;">📍</span>
                            <span style="font-size: 1.2rem; font-weight: 500;">${currentCity}</span>
                            <span style="font-size: 0.7rem;">▼</span>
                        </div>
                        <div style="font-size: 0.9rem; margin-top: 8px;">天气信息加载中...</div>
                    </div>
                    <div style="font-size: 3rem;">🌡️</div>
                </div>
                <div style="text-align: right; margin-top: -40px;">
                    <button onclick="refreshAdvice()" style="background: rgba(255,255,255,0.2); border: none; width: 32px; height: 32px; border-radius: 50%; cursor: pointer; font-size: 0.9rem; color: white;">🔄</button>
                </div>
            </div>
        `;
    }

    // 美化AI返回的建议
    let formatted = advice
        .replace(/🌿/g, '<span style="font-size: 1.2rem;">🌿</span>')
        .replace(/🍽️/g, '<span style="font-size: 1.2rem;">🍽️</span>')
        .replace(/💤/g, '<span style="font-size: 1.2rem;">💤</span>')
        .replace(/💡/g, '<span style="font-size: 1.2rem;">💡</span>')
        .replace(/📖/g, '<span style="font-size: 1.2rem;">📖</span>')
        .replace(/\n/g, '<br>');

    // 按段落分割
    let sections = formatted.split('<br><br>');
    let sectionHtml = '';

    for (let section of sections) {
        if (!section.trim()) continue;

        let bgColor = '#f8f9fc';
        if (section.includes('🌿')) bgColor = '#eef2ff';
        if (section.includes('🍽️')) bgColor = '#ecfdf5';
        if (section.includes('💤')) bgColor = '#fff7ed';
        if (section.includes('💡')) bgColor = '#fef3c7';
        if (section.includes('📖')) bgColor = '#fef2f2';

        sectionHtml += `
            <div style="background: ${bgColor}; border-radius: 16px; padding: 16px; margin-bottom: 16px; line-height: 1.7;">
                ${section}
            </div>
        `;
    }

    container.innerHTML = weatherHtml + sectionHtml;
}

function displayFallbackContent(container) {
    // 先加载天气
    loadWeatherInfo().then(() => {
        let weatherHtml = '';
        if (window.weatherData && window.weatherData.success) {
            weatherHtml = `
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 20px; padding: 20px; margin-bottom: 20px; color: white;">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-size: 1.2rem;">📍 ${window.weatherData.city}</div>
                            <div style="font-size: 1.5rem; font-weight: bold;">${window.weatherData.today?.temp || '--'}</div>
                            <div style="font-size: 0.8rem;">${window.weatherData.today?.weather || '--'}</div>
                        </div>
                        <div style="font-size: 3rem;">🌡️</div>
                    </div>
                </div>
            `;
        } else {
            weatherHtml = `
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 20px; padding: 20px; margin-bottom: 20px; color: white;">
                    <div style="display: flex; justify-content: space-between;">
                        <div><div>📍 ${currentCity}</div><div style="font-size: 0.8rem;">${getCurrentSolarTerm()}</div></div>
                        <div>🌿 ${getCurrentSeason()}</div>
                    </div>
                </div>
            `;
        }

        container.innerHTML = weatherHtml + `
            <div style="background: #eef2ff; border-radius: 16px; padding: 16px; margin-bottom: 16px;">
                <div style="font-weight: bold; margin-bottom: 10px;">🌿 养生原则</div>
                <div>${getPrincipleBySeason(getCurrentSeason())}</div>
            </div>
            <div style="background: #ecfdf5; border-radius: 16px; padding: 16px; margin-bottom: 16px;">
                <div style="font-weight: bold; margin-bottom: 10px;">🍽️ 饮食建议</div>
                <div>${getDietBySeason(getCurrentSeason())}</div>
            </div>
            <div style="background: #fff7ed; border-radius: 16px; padding: 16px; margin-bottom: 16px;">
                <div style="font-weight: bold; margin-bottom: 10px;">💤 起居运动</div>
                <div>${getLivingBySeason(getCurrentSeason())}</div>
            </div>
        `;
    });
}

function getCurrentSeason() {
    const month = new Date().getMonth() + 1;
    if (month >= 3 && month <= 5) return '春季';
    if (month >= 6 && month <= 8) return '夏季';
    if (month >= 9 && month <= 11) return '秋季';
    return '冬季';
}

function getCurrentSolarTerm() {
    const month = new Date().getMonth() + 1;
    const day = new Date().getDate();
    if (month === 4 && day >= 4) return '清明';
    if (month === 4 && day >= 20) return '谷雨';
    if (month === 5 && day >= 5) return '立夏';
    if (month === 5 && day >= 21) return '小满';
    return '春分';
}

function getPrincipleBySeason(season) {
    const map = { '春季': '春养肝，疏肝理气，升发阳气', '夏季': '夏养心，清热解暑，养心安神', '秋季': '秋养肺，滋阴润燥，收敛神气', '冬季': '冬养肾，温补肾阳，藏精养气' };
    return map[season] || '顺应四时，调和阴阳';
}

function getDietBySeason(season) {
    const map = { '春季': '多吃绿色蔬菜（韭菜、菠菜、荠菜），少吃酸味', '夏季': '多吃苦味食物（苦瓜、莲子），多喝绿豆汤', '秋季': '多吃白色食物（银耳、百合、梨），滋阴润燥', '冬季': '多吃温热食物（羊肉、生姜、红枣），适当进补' };
    return map[season] || '选择应季食材，保持饮食均衡';
}

function getLivingBySeason(season) {
    const map = { '春季': '晚睡早起，多户外活动，舒展身体', '夏季': '晚睡早起，午间小憩，避免暴晒', '秋季': '早睡早起，收敛神气，适度运动', '冬季': '早睡晚起，避寒就温，睡前泡脚' };
    return map[season] || '保持规律作息，顺应自然节律';
}

function refreshAdvice() {
    loadAllData();
}

// ✅ 确保所有函数挂载到 window
window.renderSolarPanel = renderSolarPanel;
window.toggleCityDropdown = toggleCityDropdown;
window.selectCity = selectCity;
window.addNewCity = addNewCity;
window.refreshAdvice = refreshAdvice;