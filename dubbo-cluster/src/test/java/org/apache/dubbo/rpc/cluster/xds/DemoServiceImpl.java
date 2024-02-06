package org.apache.dubbo.rpc.cluster.xds;

public class DemoServiceImpl implements DemoService{
    @Override
    public String sayHello() {
        return "hello";
    }
}
