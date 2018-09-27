package org.apache.dubbo.demo.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.demo.DemoService;

import java.io.IOException;

public class ProviderConfByAPI {
    public static void main(String[] args) throws IOException {
        DemoService demoService = new DemoServiceImpl();

        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("demo-provider");

        RegistryConfig registryConfig= new RegistryConfig();
        registryConfig.setAddress("zookeeper://127.0.0.1:2181"); //The address should be changed according to your own environment

        ProtocolConfig protocolConfig=new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20880);

        ServiceConfig<DemoService> serviceConfig=new ServiceConfig<>();
        serviceConfig.setApplication(applicationConfig);
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setInterface(DemoService.class);
        serviceConfig.setRef(demoService);
        serviceConfig.export();

        System.in.read();
    }
}
