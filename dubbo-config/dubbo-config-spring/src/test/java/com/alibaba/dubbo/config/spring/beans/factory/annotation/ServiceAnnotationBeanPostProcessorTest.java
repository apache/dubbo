package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.spring.api.DemoService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * {@link ServiceAnnotationBeanPostProcessor} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {ServiceAnnotationBeanPostProcessorTest.TestConfiguration.class})
@TestPropertySource(properties = {
        "package1 = com.alibaba.dubbo.config.spring.context.annotation",
        "packagesToScan = ${package1}"
})
public class ServiceAnnotationBeanPostProcessorTest {

    @Autowired
    private DemoService demoService;

    @Test
    public void test() {

        String value = demoService.sayName("Mercy");

        Assert.assertEquals("Hello,Mercy", value);

    }

    @ImportResource("META-INF/spring/dubbo-annotation-provider.xml")
    @PropertySource("META-INF/default.properties")
    public static class TestConfiguration {

        @Bean
        public ServiceAnnotationBeanPostProcessor serviceAnnotationBeanPostProcessor
                (@Value("${packagesToScan}") String... packagesToScan) {
            return new ServiceAnnotationBeanPostProcessor(packagesToScan);
        }


    }

}
