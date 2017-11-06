package com.alibaba.dubbo.config.spring.context.annotation.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.api.DemoService;
import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @author ken.lj
 * @date 2017/11/3
 */
@Configuration("consumerConfiguration")
@DubboComponentScan(
        basePackageClasses = ConsumerConfiguration.class
)
@ImportResource("META-INF/spring/dubbo-annotation-consumer.xml")
public class ConsumerConfiguration {

    @Reference(version = "2.5.7", url = "dubbo://127.0.0.1:12345")
    private DemoService demoService;

    public DemoService getDemoService() {
        return demoService;
    }

    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }
}
