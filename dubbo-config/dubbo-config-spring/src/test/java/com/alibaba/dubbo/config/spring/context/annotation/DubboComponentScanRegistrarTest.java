package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.config.spring.api.DemoService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

        value = consumerConfiguration.demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

        providerContext.close();
        consumerContext.close();


    }

    @DubboComponentScan(
            basePackageClasses = ConsumerConfiguration.class

    )
    @ImportResource("META-INF/spring/dubbo-annotation-consumer.xml")
    public static class ConsumerConfiguration {

        @Reference(url = "dubbo://127.0.0.1:12345")
        private DemoService demoService;

    }


    @DubboComponentScan("com.alibaba.dubbo.config.spring.beans.factory.annotation")
    @ImportResource("META-INF/spring/dubbo-annotation-provider.xml")
    public static class ProviderConfiguration {


    }


}
