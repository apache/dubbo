package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.demo.DemoService;

import java.io.IOException;

/**
 * Created by Administrator on 2018/5/30.
 */
public class Provider1 {
    public static void main(String[] args) throws IOException {
        System.setProperty("java.net.preferIPv4Stack", "true");

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-provider");

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("multicast://224.5.6.7:1234");
        applicationConfig.setRegistry(registryConfig);

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20880);
        protocolConfig.setThreads(200);

        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<DemoService>();
        DemoService demoService = new DemoServiceImpl();
        serviceConfig.setRef(demoService);
        serviceConfig.setApplication(applicationConfig);
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setInterface(DemoService.class);
//        serviceConfig.setVersion("1.0.0");

        serviceConfig.export();
        System.in.read();
    }
}
