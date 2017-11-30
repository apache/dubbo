package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;

/**
 * {@link DubboConfigBindingsRegistrar} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since DubboConfigBindingsRegistrar
 */
public class DubboConfigBindingsRegistrarTest {

    @Test
    public void test() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(TestConfig.class);

        context.refresh();

        ApplicationConfig applicationConfig = context.getBean("applicationBean", ApplicationConfig.class);

        Assert.assertEquals("dubbo-demo-application", applicationConfig.getName());

        Assert.assertEquals(2, context.getBeansOfType(ApplicationConfig.class).size());

    }


    @EnableDubboConfigBindings({
            @EnableDubboConfigBinding(prefix = "${application.prefix}", type = ApplicationConfig.class),
            @EnableDubboConfigBinding(prefix = "dubbo.application.applicationBean", type = ApplicationConfig.class)
    })
    @PropertySource("META-INF/config.properties")
    private static class TestConfig {

    }

}
