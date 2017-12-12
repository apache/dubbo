package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;

/**
 * {@link DubboConfigBindingRegistrar}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
public class DubboConfigBindingRegistrarTest {

    @Test
    public void testRegisterBeanDefinitionsForSingle() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(TestApplicationConfig.class);

        context.refresh();

        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());


    }

    @Test
    public void testRegisterBeanDefinitionsForMultiple() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(TestMultipleApplicationConfig.class);

        context.refresh();

        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());

        applicationConfig = context.getBean("applicationBean2", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application2", applicationConfig.getName());

        applicationConfig = context.getBean("applicationBean3", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application3", applicationConfig.getName());


    }

    @EnableDubboConfigBinding(prefix = "${application.prefix}", type = ApplicationConfig.class, multiple = true)
    @PropertySource("META-INF/config.properties")
    private static class TestMultipleApplicationConfig {

    }

    @EnableDubboConfigBinding(prefix = "${application.prefix}", type = ApplicationConfig.class)
    @PropertySource("META-INF/config.properties")
    private static class TestApplicationConfig {

    }


}
