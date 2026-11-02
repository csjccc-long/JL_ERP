package com.ruoyi.knowledge.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 知识中心配置（任务调度与问答线程池）
 *
 * @author ruoyi
 */
@Configuration
@EnableScheduling
public class KbConfig
{
    /**
     * 问答流式执行线程池
     *
     * @return 线程池
     */
    @Bean("kbAskExecutor")
    public ExecutorService kbAskExecutor()
    {
        AtomicInteger counter = new AtomicInteger(1);
        return new ThreadPoolExecutor(4, 16, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(200),
                runnable -> {
                    Thread thread = new Thread(runnable, "kb-ask-" + counter.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }
}
