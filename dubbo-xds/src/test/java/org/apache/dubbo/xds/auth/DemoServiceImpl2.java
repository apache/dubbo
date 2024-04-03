package org.apache.dubbo.xds.auth;

import org.apache.dubbo.rpc.RpcContext;

public class DemoServiceImpl2 implements DemoService2 {
    @Override
    public String sayHello(String name) {
        System.out.println("service2 impl get attachment:"+ RpcContext.getServerAttachment().getAttachment("s1"));
        return "hello:"+name;
    }
}
