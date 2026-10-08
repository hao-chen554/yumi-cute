package com.yumi.cute.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("photoTaskExecutor")
    public ThreadPoolTaskExecutor photoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);          // 常驻核心线程数
        executor.setMaxPoolSize(8);           // 最多能开到几个线程
        executor.setQueueCapacity(50);        // 排队队列长度
        executor.setKeepAliveSeconds(60);     // 超出核心数后，空闲线程存活多久
        executor.setThreadNamePrefix("photo-gen-");   // 线程名前缀，看日志时很有用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}