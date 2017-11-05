package com.alibaba.dubbo.config;

import org.junit.Test;

import java.util.Arrays;

import static com.alibaba.dubbo.config.RegistryConfig.NO_AVAILABLE;

/**
 * @author panlingxiao
 */
public class ServiceConfigTest {

    public interface HelloService{
        void myMethod(String str1,String str2);
        void myMethod(String str1,Object str2);
    }

    public class HelloServiceImpl implements HelloService{

        @Override
        public void myMethod(String str1, String str2) {

        }

        @Override
        public void myMethod(String str1, Object str2) {

        }
    }

    @Test
    public void test_ArgumentConfig(){
        ApplicationConfig application = new ApplicationConfig("argument-test");
        RegistryConfig registry = new RegistryConfig();
        registry.setAddress(NO_AVAILABLE);

        MethodConfig methodConfig = new MethodConfig();
        ArgumentConfig arg1 = new ArgumentConfig();
        arg1.setCallback(true);
        arg1.setIndex(0);
        arg1.setType(String.class.getName());

        ArgumentConfig arg2 = new ArgumentConfig();
        arg2.setType(Object.class.getName());
        arg2.setIndex(1);
        arg2.setCallback(true);

        methodConfig.setName("myMethod");
        methodConfig.setArguments(Arrays.asList(arg1,arg2));

        ServiceConfig<HelloService> serviceConfig = new ServiceConfig<HelloService>();
        serviceConfig.setApplication(application);
        serviceConfig.setRegistry(registry);
        serviceConfig.setInterface(HelloService.class);
        serviceConfig.setRef(new HelloServiceImpl());
        serviceConfig.setMethods(Arrays.asList(methodConfig));

        serviceConfig.export();
    }
}
