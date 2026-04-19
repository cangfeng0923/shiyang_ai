// service/RedisCacheService.java
package org.example.shiyangai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 统一Key前缀
    private static final String FOOD_INFO_PREFIX = "food:info:";
    private static final String USER_PROFILE_PREFIX = "user:profile:";
    private static final String CHAT_CONTEXT_PREFIX = "chat:context:";
    private static final String SOLAR_TERM_PREFIX = "solar:term:";
    private static final String DIET_REPORT_PREFIX = "diet:report:";

    /**
     * 缓存食材信息（30天）
     */
    public void cacheFoodInfo(String foodName, Object info) {
        String key = FOOD_INFO_PREFIX + foodName.toLowerCase();
        redisTemplate.opsForValue().set(key, info, 30, TimeUnit.DAYS);
        log.debug("缓存食材: {}", foodName);
    }

    /**
     * 获取缓存的食材信息
     */
    public Object getFoodInfo(String foodName) {
        String key = FOOD_INFO_PREFIX + foodName.toLowerCase();
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 缓存用户健康档案（1小时）
     */
    public void cacheUserProfile(String userId, Object profile) {
        String key = USER_PROFILE_PREFIX + userId;
        redisTemplate.opsForValue().set(key, profile, 1, TimeUnit.HOURS);
    }

    /**
     * 获取用户健康档案缓存
     */
    public Object getUserProfile(String userId) {
        String key = USER_PROFILE_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 缓存用户会话上下文（30分钟）
     */
    public void cacheChatContext(String userId, String context) {
        String key = CHAT_CONTEXT_PREFIX + userId;
        redisTemplate.opsForValue().set(key, context, 30, TimeUnit.MINUTES);
        log.debug("缓存对话上下文: userId={}", userId);
    }

    /**
     * 获取用户会话上下文
     */
    public String getChatContext(String userId) {
        String key = CHAT_CONTEXT_PREFIX + userId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 缓存节气信息（24小时）
     */
    public void cacheSolarTerm(String termKey, Object termInfo) {
        String key = SOLAR_TERM_PREFIX + termKey;
        redisTemplate.opsForValue().set(key, termInfo, 24, TimeUnit.HOURS);
    }

    /**
     * 获取节气缓存
     */
    public Object getSolarTerm(String termKey) {
        String key = SOLAR_TERM_PREFIX + termKey;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 缓存饮食报告（1小时）
     */
    public void cacheDietReport(String userId, Object report) {
        String key = DIET_REPORT_PREFIX + userId;
        redisTemplate.opsForValue().set(key, report, 1, TimeUnit.HOURS);
    }

    /**
     * 获取饮食报告缓存
     */
    public Object getDietReport(String userId) {
        String key = DIET_REPORT_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除用户相关缓存
     */
    public void clearUserCache(String userId) {
        String profileKey = USER_PROFILE_PREFIX + userId;
        String chatKey = CHAT_CONTEXT_PREFIX + userId;
        String reportKey = DIET_REPORT_PREFIX + userId;

        redisTemplate.delete(profileKey);
        redisTemplate.delete(chatKey);
        redisTemplate.delete(reportKey);
        log.info("清除用户缓存: userId={}", userId);
    }

    /**
     * 通用存值
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 通用取值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 检查Key是否存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}