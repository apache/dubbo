package com.alibaba.dubbo.config.spring.context.annotation.consumer;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.api.DemoService;
import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 * @author ken.lj
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @date 2017/11/3
 */
@Configuration("consumerConfiguration")
@DubboComponentScan(
        basePackageClasses = ConsumerConfiguration.class
)
@PropertySource("META-INF/default.properties")
public class ConsumerConfiguration {

    /**
     * 当前应用配置，替代 XML 方式配置：
     * <prev>
     * &lt;dubbo:application name="dubbo-annotation-consumer"/&gt;
     * </prev>
     *
     * @return {@link ApplicationConfig} Bean
     */
    @Bean("dubbo-annotation-consumer")
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("dubbo-annotation-consumer");
        return applicationConfig;
    }

    /**
     * 当前连接注册中心配置，替代 XML 方式配置：
     * <prev>
     * &lt;dubbo:registry address="N/A"/&gt;
     * </prev>
     *
     * @return {@link RegistryConfig} Bean
     */
    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("N/A");
        return registryConfig;
    }

    @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345")
    private DemoService demoService;

    public DemoService getDemoService() {
        return demoService;
    }

    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }


    @Bean
    public Child c() {
        return new Child();
    }

    public static abstract class Ancestor {

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345")
        private DemoService demoServiceFromAncestor;

        public DemoService getDemoServiceFromAncestor() {
            return demoServiceFromAncestor;
        }

        public void setDemoServiceFromAncestor(DemoService demoServiceFromAncestor) {
            this.demoServiceFromAncestor = demoServiceFromAncestor;
        }
    }

    public static abstract class Parent extends Ancestor {

        private DemoService demoServiceFromParent;

        public DemoService getDemoServiceFromParent() {
            return demoServiceFromParent;
        }

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345")
        public void setDemoServiceFromParent(DemoService demoServiceFromParent) {
            this.demoServiceFromParent = demoServiceFromParent;
        }

    }

    public static class Child extends Parent {

        @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345")
        private DemoService demoServiceFromChild;

        public DemoService getDemoServiceFromChild() {
            return demoServiceFromChild;
        }

        public void setDemoServiceFromChild(DemoService demoServiceFromChild) {
            this.demoServiceFromChild = demoServiceFromChild;
        }
    }

}
