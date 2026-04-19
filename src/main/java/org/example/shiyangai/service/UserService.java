package org.example.shiyangai.service;


//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.shiyangai.entity.FoodHistory;
import org.example.shiyangai.entity.User;
import org.example.shiyangai.mapper.FoodHistoryMapper;
import org.example.shiyangai.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final FoodHistoryMapper foodHistoryMapper;

    // 注册
    public String register(String username, String password) {
        // 检查用户名是否存在
        User existing = userMapper.selectByUsername(username);
        if (existing != null) {
            return null; // 用户已存在
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        user.setUsername(username);
        user.setPassword(password); // 演示用，实际应该加密
        user.setCreatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user.getId();
    }

    // 登录
    public User login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }


    /**
     * 获取或创建用户
     * @param userId 前端传的userId（可能为null）
     * @return 有效的userId
     */
//    public String getOrCreateUser(String userId) {
//        if (userId != null && !userId.isEmpty()) {
//            User user = userMapper.selectById(userId);
//            if (user != null) {
//                log.debug("用户已存在: {}", userId);
//                return userId;
//            }
//        }
//
//        // 创建新用户
//        String newUserId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
//        log.info("创建新用户: {}", newUserId);
//        return newUserId;
//    }

    /**
     * 保存用户体质
     */
    @Transactional
    public void saveConstitution(String userId, String constitution) {
        User user = new User();
        user.setId(userId);
        user.setConstitution(constitution);
        user.setUpdatedAt(LocalDateTime.now());

        User existing = userMapper.selectById(userId);
        if (existing != null) {
            userMapper.updateById(user);
            log.info("更新用户体质: userId={}, constitution={}", userId, constitution);
        } else {
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
            log.info("新增用户体质: userId={}, constitution={}", userId, constitution);
        }
    }

    /**
     * 获取用户体质
     * @return 体质名称，如果不存在返回null
     */
    public String getConstitution(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        User user = userMapper.selectById(userId);
        String constitution = user != null ? user.getConstitution() : null;
        log.debug("获取用户体质: userId={}, constitution={}", userId, constitution);
        return constitution;
    }

    /**
     * 检查用户是否已完成测评
     */
    public boolean hasAssessment(String userId) {
        return getConstitution(userId) != null;
    }

    /**
     * 保存饮食历史
     */
    @Transactional
    public void saveFoodHistory(String userId, String foodName, String constitution,
                                String suitability, String suggestion, String nutritionData) {
        FoodHistory history = new FoodHistory();
        history.setUserId(userId);
        history.setFoodName(foodName);
        history.setConstitution(constitution);
        history.setSuitability(suitability);
        history.setSuggestion(suggestion);
        history.setNutritionData(nutritionData);
        history.setCreatedAt(LocalDateTime.now());

        foodHistoryMapper.insert(history);
        log.info("保存饮食历史: userId={}, foodName={}, suitability={}", userId, foodName, suitability);
    }

    /**
     * 获取用户饮食历史（最近10条）
     */
    public List<FoodHistory> getFoodHistory(String userId, int limit) {
        LambdaQueryWrapper<FoodHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodHistory::getUserId, userId)
                .orderByDesc(FoodHistory::getCreatedAt)
                .last("LIMIT " + limit);

        return foodHistoryMapper.selectList(wrapper);
    }

    /**
     * 获取用户饮食统计
     */
    public List<FoodHistory> getUserStats(String userId) {
        LambdaQueryWrapper<FoodHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FoodHistory::getUserId, userId)
                .orderByDesc(FoodHistory::getCreatedAt)
                .last("LIMIT 50");

        return foodHistoryMapper.selectList(wrapper);
    }

    public User getUserById(String userId) {
        return userMapper.selectByUserId(userId);
    }

    /**
     * 获取或创建用户
     * 如果userId为空，创建新用户
     * 如果userId不为空但不存在，创建新用户并返回新ID
     * 如果userId存在，直接返回
     *
     * @param userId 用户ID（可选）
     * @return 用户ID
     */
    public String getOrCreateUser(String userId) {
        // 情况1：userId为空，直接创建新用户
        if (userId == null || userId.isEmpty()) {
            return createNewUser();
        }

        // 情况2：userId不为空，检查是否存在
        User existingUser = userMapper.selectById(userId);
        if (existingUser != null) {
            log.debug("用户已存在: userId={}", userId);
            return userId;
        }

        // 情况3：userId不为空但不存在，创建新用户（使用传入的userId）
        log.info("用户不存在，创建新用户: userId={}", userId);
        createUserWithId(userId);
        return userId;
    }

    /**
     * 创建新用户（自动生成UUID）
     *
     * @return 新用户ID
     */
    private String createNewUser() {
        String userId = generateUserId();
        createUserWithId(userId);
        return userId;
    }

    /**
     * 使用指定ID创建用户
     *
     * @param userId 用户ID
     */
    private void createUserWithId(String userId) {
        User user = new User();
        user.setId(userId);
        user.setConstitution(null);  // 初始体质为空，需要先测评
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        int result = userMapper.insert(user);
        if (result > 0) {
            log.info("创建用户成功: userId={}", userId);
        } else {
            log.error("创建用户失败: userId={}", userId);
            throw new RuntimeException("创建用户失败");
        }
    }

    /**
     * 生成用户ID（使用hutool或Java自带UUID）
     *
     * @return 用户ID
     */
    private String generateUserId() {
        // 方式1：使用hutool（需要添加依赖）
        // return UUID.fastUUID().toString(true);  // 不带横线

        // 方式2：使用Java原生UUID（推荐，无需额外依赖）
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 方式3：带时间戳的ID
        // return "user_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 更新用户体质
     *
     * @param userId       用户ID
     * @param constitution 体质类型
     */
    public void updateUserConstitution(String userId, String constitution) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setConstitution(constitution);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("更新用户体质: userId={}, constitution={}", userId, constitution);
        } else {
            log.warn("用户不存在，无法更新体质: userId={}", userId);
        }
    }

    /**
     * 获取用户体质
     *
     * @param userId 用户ID
     * @return 体质类型，如果用户不存在或未测评返回null
     */
    public String getUserConstitution(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user != null ? user.getConstitution() : null;
    }

    /**
     * 检查用户是否存在
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean userExists(String userId) {
        if (userId == null || userId.isEmpty()) {
            return false;
        }
        return userMapper.selectById(userId) != null;
    }

    /**
     * 根据ID获取用户（使用MyBatis-Plus）
     */
    public User selectById(String id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据用户名获取用户
     */
    public User selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
}