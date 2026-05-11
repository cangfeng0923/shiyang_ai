// ThreadPoolConfig.java
package org.example.shiyangai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    @Bean("aiExecutorService")
    public ExecutorService aiExecutorService() {
        return new ThreadPoolExecutor(
                4,                          // corePoolSize
                8,                          // maximumPoolSize
                60L,                        // keepAliveTime
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),  // 有界队列
                new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
        );
    }
}