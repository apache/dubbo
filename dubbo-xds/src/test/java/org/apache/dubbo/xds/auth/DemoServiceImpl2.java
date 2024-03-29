package org.apache.dubbo.xds.auth;

public class DemoServiceImpl2 implements DemoService2 {
    @Override
    public String sayHello(String name) {
        return "hello:"+name;
    }
}
