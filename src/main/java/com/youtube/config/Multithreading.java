package com.youtube.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class Multithreading {

    @Bean(name = "videoTaskExecutor")
    public Executor videoTaskExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Configuration parameters
        executor.setCorePoolSize(5);      // Minimum active threads
        executor.setMaxPoolSize(10);     // Maximum threads if queue is full
        executor.setQueueCapacity(100);  // Queue mein kitni videos wait kar sakti hain
        executor.setThreadNamePrefix("VideoWorker-");
        executor.initialize();
        return  executor;
    }
}
