package org.apache.dubbo.demo.provider;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;
import org.apache.dubbo.demo.GreeterStreamService;
import org.apache.dubbo.demo.GreeterStreamServiceImpl;

public class ApiStreamProvider {
    public static void main(String[] args) throws InterruptedException {
        ServiceConfig<GreeterStreamService> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(GreeterStreamService.class);
        serviceConfig.setRef(new GreeterStreamServiceImpl());
        serviceConfig.setTimeout(1000 * 60 * 30);

        DubboBootstrap bootstrap = DubboBootstrap.getInstance();
        bootstrap
                .application(new ApplicationConfig("dubbo-demo-triple-api-provider"))
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(new ProtocolConfig(CommonConstants.TRIPLE, -1))
                .service(serviceConfig)
                .start()
                .await();
    }
}
