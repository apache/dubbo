package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.demo.DemoService;

import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2018/5/30.
 */
public class Consumer1 {
    public static void main(String[] args) throws InterruptedException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-consumer");

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("multicast://224.5.6.7:1234");
        applicationConfig.setRegistry(registryConfig);

        ReferenceConfig<DemoService> referenceConfig = new ReferenceConfig<DemoService>();
        referenceConfig.setApplication(applicationConfig);
        referenceConfig.setRegistry(registryConfig);
        referenceConfig.setCheck(true);
        referenceConfig.setInterface(DemoService.class);
//        referenceConfig.setVersion("1.0.0");

        DemoService demoService = referenceConfig.get();
        while (true) {
            String result = demoService.sayHello("world");
            System.out.println(result);
            TimeUnit.SECONDS.sleep(3);
        }
    }
}
