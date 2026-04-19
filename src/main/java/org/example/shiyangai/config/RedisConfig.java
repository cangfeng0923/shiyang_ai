package org.example.shiyangai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 String 序列化器作为 Key
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // 使用 JSON 序列化器作为 Value
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();

        // 测试连接
        try {
            template.opsForValue().set("test:connection", "ok");
            // 从连接工厂配置中直接获取数据库索引
            RedisConnectionFactory factory = template.getConnectionFactory();
            RedisStandaloneConfiguration config = (RedisStandaloneConfiguration)
                    ((LettuceConnectionFactory) factory).getStandaloneConfiguration();
            int dbIndex = config.getDatabase();

            System.out.println("Redis连接成功，使用DB: " + dbIndex);
//            System.out.println("Redis连接成功，使用DB: " + template.getConnectionFactory().getConnection().getClass());
        } catch (Exception e) {
            System.err.println("Redis连接失败: " + e.getMessage());
        }

        return template;
    }
}