package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.api.DemoService;
import com.alibaba.dubbo.config.spring.context.annotation.consumer.test.TestConsumerConfiguration;
import com.alibaba.dubbo.config.spring.context.annotation.provider.DemoServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * {@link EnableDubbo} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
public class EnableDubboTest {

    @Test
    public void test() {

        AnnotationConfigApplicationContext providerContext = new AnnotationConfigApplicationContext();

        providerContext.register(TestProviderConfiguration.class);

        providerContext.refresh();

        DemoService demoService = providerContext.getBean(DemoService.class);

        String value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        Class<?> beanClass = AopUtils.getTargetClass(demoService);

        // DemoServiceImpl with @Transactional
        Assert.assertEquals(DemoServiceImpl.class, beanClass);

        // Test @Transactional is present or not
        Assert.assertNotNull(findAnnotation(beanClass, Transactional.class));

        AnnotationConfigApplicationContext consumerContext = new AnnotationConfigApplicationContext();

        consumerContext.register(TestConsumerConfiguration.class);

        consumerContext.refresh();

        TestConsumerConfiguration consumerConfiguration = consumerContext.getBean(TestConsumerConfiguration.class);

        demoService = consumerConfiguration.getDemoService();

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        TestConsumerConfiguration.Child child = consumerContext.getBean(TestConsumerConfiguration.Child.class);

        // From Child

        demoService = child.getDemoServiceFromChild();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        // From Parent

        demoService = child.getDemoServiceFromParent();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        // From Ancestor

        demoService = child.getDemoServiceFromAncestor();

        Assert.assertNotNull(demoService);

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        // Test dubbo-annotation-consumer2 bean presentation

        ApplicationConfig applicationConfig = consumerContext.getBean("dubbo-annotation-consumer2", ApplicationConfig.class);

        // Test multiple binding
        Assert.assertEquals("dubbo-consumer2", applicationConfig.getName());

        // Test dubbo-annotation-consumer2 bean presentation
        RegistryConfig registryConfig = consumerContext.getBean("my-registry2", RegistryConfig.class);

        // Test multiple binding
        Assert.assertEquals("N/A", registryConfig.getAddress());

        providerContext.close();
        consumerContext.close();


    }

    @EnableDubbo(scanBasePackages = "com.alibaba.dubbo.config.spring.context.annotation.provider")
    @ComponentScan(basePackages = "com.alibaba.dubbo.config.spring.context.annotation.provider")
    @PropertySource("META-INF/dubbb-provider.properties")
    @EnableTransactionManagement
    public static class TestProviderConfiguration {

        @Primary
        @Bean
        public PlatformTransactionManager platformTransactionManager() {
            return new PlatformTransactionManager() {

                @Override
                public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
                    return null;
                }

                @Override
                public void commit(TransactionStatus status) throws TransactionException {

                }

                @Override
                public void rollback(TransactionStatus status) throws TransactionException {

                }
            };
        }
    }


}
