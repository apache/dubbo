package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.spring.api.DemoService;
import com.alibaba.dubbo.config.spring.context.annotation.consumer.ConsumerConfiguration;
import com.alibaba.dubbo.config.spring.context.annotation.provider.DemoServiceImpl;
import com.alibaba.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

/**
 * {@link DubboComponentScanRegistrar} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
public class DubboComponentScanRegistrarTest {

    @Test
    public void test() {

        AnnotationConfigApplicationContext providerContext = new AnnotationConfigApplicationContext();

        providerContext.register(ProviderConfiguration.class);

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

        consumerContext.register(ConsumerConfiguration.class);

        consumerContext.refresh();

        ConsumerConfiguration consumerConfiguration = consumerContext.getBean(ConsumerConfiguration.class);

        demoService = consumerConfiguration.getDemoService();

        value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        ConsumerConfiguration.Child child = consumerContext.getBean(ConsumerConfiguration.Child.class);

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

        providerContext.close();
        consumerContext.close();


    }


}
