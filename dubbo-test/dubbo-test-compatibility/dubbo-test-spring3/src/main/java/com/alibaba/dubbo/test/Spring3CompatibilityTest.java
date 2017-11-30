package com.alibaba.dubbo.test;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.test.consumer.ConsumerConfiguration;
import com.alibaba.dubbo.test.provider.ProviderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.Assert;

/**
 * Dubbo compatibility test on Spring 3.2.x
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
public class Spring3CompatibilityTest {

    public static void main(String[] args) {

        ConfigurableApplicationContext provider = startupProvider();

        ConfigurableApplicationContext consumer = startConsumer();

        ConsumerConfiguration consumerConfiguration = consumer.getBean(ConsumerConfiguration.class);

        DemoService demoService = consumerConfiguration.getDemoService();

        String value = demoService.sayHello("Mercy");

        Assert.isTrue("DefaultDemoService - sayHell() : Mercy".equals(value), "Test is failed!");

        System.out.println(value);

        provider.close();
        consumer.close();

    }

    private static ConfigurableApplicationContext startupProvider() {

        ConfigurableApplicationContext context = startupApplicationContext(ProviderConfiguration.class);

        System.out.println("Startup Provider ...");

        return context;
    }

    private static ConfigurableApplicationContext startConsumer() {

        ConfigurableApplicationContext context = startupApplicationContext(ConsumerConfiguration.class);

        System.out.println("Startup Consumer ...");

        return context;

    }

    private static ConfigurableApplicationContext startupApplicationContext(Class<?>... annotatedClasses) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(annotatedClasses);
        context.refresh();
        return context;
    }

}
