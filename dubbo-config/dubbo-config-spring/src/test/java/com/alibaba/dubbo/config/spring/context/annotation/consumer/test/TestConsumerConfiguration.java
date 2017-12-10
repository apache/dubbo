package com.alibaba.dubbo.config.spring.context.annotation.consumer.test;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.api.DemoService;
import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test Consumer Configuration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.7
 */
@EnableDubbo(scanBasePackageClasses = TestConsumerConfiguration.class, multipleConfig = true)
@PropertySource("META-INF/dubbb-consumer.properties")
@EnableTransactionManagement
public class TestConsumerConfiguration {

    @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345", application = "dubbo-annotation-consumer")
    private DemoService demoService;

    public DemoService getDemoService() {
        return demoService;
    }

    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }


    @Bean
    public TestConsumerConfiguration.Child c() {
        return new TestConsumerConfiguration.Child();
    }

    public static abstract class Ancestor {

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345", application = "dubbo-annotation-consumer")
        private DemoService demoServiceFromAncestor;

        public DemoService getDemoServiceFromAncestor() {
            return demoServiceFromAncestor;
        }

        public void setDemoServiceFromAncestor(DemoService demoServiceFromAncestor) {
            this.demoServiceFromAncestor = demoServiceFromAncestor;
        }
    }

    public static abstract class Parent extends TestConsumerConfiguration.Ancestor {

        private DemoService demoServiceFromParent;

        public DemoService getDemoServiceFromParent() {
            return demoServiceFromParent;
        }

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345", application = "dubbo-annotation-consumer")
        public void setDemoServiceFromParent(DemoService demoServiceFromParent) {
            this.demoServiceFromParent = demoServiceFromParent;
        }

    }

    public static class Child extends TestConsumerConfiguration.Parent {

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345", application = "dubbo-annotation-consumer")
        private DemoService demoServiceFromChild;

        public DemoService getDemoServiceFromChild() {
            return demoServiceFromChild;
        }

        public void setDemoServiceFromChild(DemoService demoServiceFromChild) {
            this.demoServiceFromChild = demoServiceFromChild;
        }
    }
}
