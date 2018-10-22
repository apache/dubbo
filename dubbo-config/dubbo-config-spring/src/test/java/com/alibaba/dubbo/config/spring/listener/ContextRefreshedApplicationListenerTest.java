package com.alibaba.dubbo.config.spring.listener;

import com.alibaba.dubbo.common.LockSwitch;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.StaticApplicationContext;

/**
 * @author cvictory ON 2018/10/22
 */
public class ContextRefreshedApplicationListenerTest {

    ContextRefreshedApplicationListener contextRefreshedApplicationListener = new ContextRefreshedApplicationListener();

    @Test
    public void testNotContextRefreshedEvent(){
        Assert.assertTrue(LockSwitch.INIT_TASK_NUM.get() == 0);
        contextRefreshedApplicationListener.onApplicationEvent(new InnerTestApplicationEvent("source"));
        Assert.assertFalse(contextRefreshedApplicationListener.EXECUTED.get());
        Assert.assertTrue(LockSwitch.INIT_TASK_NUM.get() == 0);
    }

    @Test
    public void testContextRefreshedEvent(){
        Assert.assertTrue(LockSwitch.INIT_TASK_NUM.get() == 0);
        contextRefreshedApplicationListener.onApplicationEvent(new ContextRefreshedEvent(new StaticApplicationContext()));
        Assert.assertTrue(contextRefreshedApplicationListener.EXECUTED.get());
        Assert.assertTrue(LockSwitch.INIT_TASK_NUM.get() == -1);
        //rediscovery
        LockSwitch.INIT_TASK_NUM.set(0);
    }

    @Test
    public void testContextRefreshedEventTwice(){
        Assert.assertTrue(LockSwitch.INIT_TASK_NUM.get() == 0);
        ApplicationContext applicationContext = new StaticApplicationContext();
        contextRefreshedApplicationListener.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
        Assert.assertTrue(contextRefreshedApplicationListener.EXECUTED.get());
        Assert.assertTrue(LockSwitch.INIT_TASK_NUM.get() == -1);
        contextRefreshedApplicationListener.onApplicationEvent(new ContextRefreshedEvent(applicationContext));
        Assert.assertTrue(contextRefreshedApplicationListener.EXECUTED.get());
        Assert.assertTrue(LockSwitch.INIT_TASK_NUM.get() == -1);

        //rediscovery
        LockSwitch.INIT_TASK_NUM.set(0);
    }


    private static class InnerTestApplicationEvent extends ApplicationEvent {

        /**
         * Create a new ApplicationEvent.
         *
         * @param source the object on which the event initially occurred (never {@code null})
         */
        public InnerTestApplicationEvent(Object source) {
            super(source);
        }
    }
}
