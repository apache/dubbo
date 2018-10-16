package com.alibaba.dubbo.config.spring.listener;

import com.alibaba.dubbo.common.LockSwitch;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 *
 * 2018/10/16
 */
public class ContextRefreshedApplicationListener implements ApplicationListener<ApplicationEvent> {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            LockSwitch.INIT_TASK_NUM.decrementAndGet();
        }
    }
}
