// API基础地址
const API_BASE = '';

// 全局变量
let currentUser = null;

// 工具函数
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    if (container) container.scrollTop = container.scrollHeight;
}

function showMessage(msg, type) {
    alert(msg); // 简单实现，可替换为更好的提示组件
}

// 体质头像映射
function getAvatarByConstitution(constitution) {
    const map = {
        '气虚质': '😫', '阳虚质': '❄️', '阴虚质': '🔥',
        '痰湿质': '💧', '湿热质': '🌊', '血瘀质': '🩸',
        '气郁质': '😔', '特禀质': '🤧', '平和质': '🌱'
    };
    return map[constitution] || '👤';
}

// 体质建议映射
function getConstitutionAdvice(constitution) {
    const advice = {
        '气虚质': '补气健脾，多吃山药、大枣、黄芪炖鸡',
        '阳虚质': '温补阳气，多吃羊肉、生姜、桂圆',
        '阴虚质': '滋阴润燥，多吃百合、银耳、梨',
        '痰湿质': '健脾祛湿，吃薏米红豆粥、多运动',
        '湿热质': '清热利湿，多吃绿豆、冬瓜、苦瓜',
        '血瘀质': '活血化瘀，多吃山楂、黑豆、玫瑰花茶',
        '气郁质': '理气解郁，喝玫瑰花茶、多交朋友',
        '特禀质': '避免过敏原，多吃蜂蜜、大枣',
        '平和质': '身体底子不错，保持良好生活习惯即可'
    };
    return advice[constitution] || '有什么健康问题可以随时问我。';
}

// 餐次名称映射
function getMealName(mealType) {
    const map = { 'BREAKFAST': '🌅 早餐', 'LUNCH': '☀️ 午餐', 'DINNER': '🌙 晚餐', 'SNACK': '🍪 加餐' };
    return map[mealType] || mealType;
}

// API基础地址
const API_BASE = '';

// 全局变量
let currentUser = null;

// 工具函数
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    if (container) container.scrollTop = container.scrollHeight;
}

function showMessage(msg, type) {
    alert(msg); // 简单实现，可替换为更好的提示组件
}

// 体质头像映射
function getAvatarByConstitution(constitution) {
    const map = {
        '气虚质': '😫', '阳虚质': '❄️', '阴虚质': '🔥',
        '痰湿质': '💧', '湿热质': '🌊', '血瘀质': '🩸',
        '气郁质': '😔', '特禀质': '🤧', '平和质': '🌱'
    };
    return map[constitution] || '👤';
}

// 体质建议映射
function getConstitutionAdvice(constitution) {
    const advice = {
        '气虚质': '补气健脾，多吃山药、大枣、黄芪炖鸡',
        '阳虚质': '温补阳气，多吃羊肉、生姜、桂圆',
        '阴虚质': '滋阴润燥，多吃百合、银耳、梨',
        '痰湿质': '健脾祛湿，吃薏米红豆粥、多运动',
        '湿热质': '清热利湿，多吃绿豆、冬瓜、苦瓜',
        '血瘀质': '活血化瘀，多吃山楂、黑豆、玫瑰花茶',
        '气郁质': '理气解郁，喝玫瑰花茶、多交朋友',
        '特禀质': '避免过敏原，多吃蜂蜜、大枣',
        '平和质': '身体底子不错，保持良好生活习惯即可'
    };
    return advice[constitution] || '有什么健康问题可以随时问我。';
}

// 餐次名称映射
function getMealName(mealType) {
    const map = { 'BREAKFAST': '🌅 早餐', 'LUNCH': '☀️ 午餐', 'DINNER': '🌙 晚餐', 'SNACK': '🍪 加餐' };
    return map[mealType] || mealType;
}