package com.youtube.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class Multithreading {

    @Bean(name = "videoTaskExecutor")
    public Executor videoTaskExecutor(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
