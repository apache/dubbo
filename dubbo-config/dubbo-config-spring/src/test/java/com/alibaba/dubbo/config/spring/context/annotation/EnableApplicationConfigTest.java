package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;

/**
 * {@link EnableApplicationConfig} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
public class EnableApplicationConfigTest {

    @Test
    public void test() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(TestConfig.class);

        context.refresh();

        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());
    }

    @EnableApplicationConfig
    @PropertySource("META-INF/config.properties")
    private static class TestConfig {

    }
}
