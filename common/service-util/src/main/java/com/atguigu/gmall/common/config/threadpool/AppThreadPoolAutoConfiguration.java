package com.atguigu.gmall.common.config.threadpool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 配置线程池
 */
@EnableConfigurationProperties(AppThreadPoolProperties.class)
@Configuration
public class AppThreadPoolAutoConfiguration {
    @Autowired
    AppThreadPoolProperties threadPoolProperties;

    @Value("${spring.application.name}")
    String applicationName;

    /*
       ArrayBlockingQueue<>() 底层对列是一个数组
       LinkedBlockingDeque<>() 底层对列是一个链表
       数组与链表？ --检索、插入
       数组是连续空间，链表不连续（利用碎片化空间）
    */
    @Bean
    public ThreadPoolExecutor coreExecutor(){
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadPoolProperties.getCore(),
                threadPoolProperties.getMax(),
                threadPoolProperties.getKeepAliveTime(),
                TimeUnit.MINUTES,
                new LinkedBlockingDeque<>(threadPoolProperties.getQueueSize()),//对列的大小由项目最终能占的最大内存决定
                new ThreadFactory() {
                    int i = 0; //记录线程自增id
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName(applicationName + "[core-thread-"+ i++ +"]");
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return executor;
    }
}
