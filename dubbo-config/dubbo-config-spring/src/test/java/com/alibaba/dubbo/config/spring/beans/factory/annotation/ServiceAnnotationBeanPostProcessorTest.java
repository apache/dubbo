package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.spring.api.DemoService;
import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ImportResource;

/**
 * {@link ServiceAnnotationBeanPostProcessor} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.7
 */
public class ServiceAnnotationBeanPostProcessorTest {

    @Test
    public void test() {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.register(Config.class);

        context.refresh();

        DemoService demoService = context.getBean(DemoService.class);

        Assert.assertEquals("Hello,Mercy", demoService.sayName("Mercy"));

        context.close();

    }


    @DubboComponentScan(basePackages = {
            "com.alibaba.dubbo.config.spring.beans.factory.annotation",
            "com.alibaba.dubbo.config.spring.context.annotation"})
    @ImportResource("META-INF/spring/dubbo-annotation-provider.xml")
    public static class Config {


    }

}
