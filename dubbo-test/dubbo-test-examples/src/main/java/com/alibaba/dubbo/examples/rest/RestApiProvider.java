package com.alibaba.dubbo.examples.rest;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.examples.rest.api.facade.UserRestService;
import com.alibaba.dubbo.examples.rest.impl.facade.UserRestServiceImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author guanghao on 30/11/2017.
 */
public class RestApiProvider {
    public static void main(String[] args) throws IOException {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("dubbo-provider");

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://127.0.0.1:2181");

        ProtocolConfig restProtocol = new ProtocolConfig();
        restProtocol.setName("rest");
        restProtocol.setPort(8080);
        restProtocol.setThreads(200);
        restProtocol.setServer("tomcat");
        restProtocol.setKeepAlive(true);
        restProtocol.setExtension("com.alibaba.dubbo.examples.rest.api.extension.TraceInterceptor,\n" +
                "                    com.alibaba.dubbo.examples.rest.api.extension.TraceFilter,\n" +
                "                    com.alibaba.dubbo.examples.rest.api.extension.ClientTraceFilter,\n" +
                "                    com.alibaba.dubbo.examples.rest.api.extension.DynamicTraceBinding,\n" +
                "                    com.alibaba.dubbo.examples.rest.api.extension.CustomExceptionMapper,\n" +
                "                    com.alibaba.dubbo.rpc.protocol.rest.support.LoggingFilter");
//
        ProtocolConfig dubboProtocol = new ProtocolConfig();
        dubboProtocol.setName("dubbo");
        dubboProtocol.setPort(8081);

        List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
        protocolConfigs.add(restProtocol);
        protocolConfigs.add(dubboProtocol);

        ServiceConfig<UserRestService> serviceConfig = new ServiceConfig<UserRestService>();
        serviceConfig.setApplication(applicationConfig);
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setRef(new UserRestServiceImpl());
        serviceConfig.setProtocols(protocolConfigs);
        serviceConfig.setInterface(UserRestService.class);
        serviceConfig.setFilter("logFilter");

        serviceConfig.export();

        System.in.read();

    }
}
