package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.spring.api.DemoService;
import com.alibaba.dubbo.config.spring.context.annotation.consumer.ConsumerConfiguration;
import com.alibaba.dubbo.config.spring.context.annotation.provider.ProviderConfiguration;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * {@link DubboComponentScanRegistrar} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.7
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

        AnnotationConfigApplicationContext consumerContext = new AnnotationConfigApplicationContext();

        consumerContext.register(ConsumerConfiguration.class);

        consumerContext.refresh();

        ConsumerConfiguration consumerConfiguration = consumerContext.getBean(ConsumerConfiguration.class);

        value = consumerConfiguration.getDemoService().sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        providerContext.close();
        consumerContext.close();


    }


}
