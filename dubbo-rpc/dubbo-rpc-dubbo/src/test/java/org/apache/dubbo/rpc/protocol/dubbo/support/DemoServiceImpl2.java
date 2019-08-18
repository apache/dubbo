package org.apache.dubbo.rpc.protocol.dubbo.support;

public class DemoServiceImpl2 extends DemoServiceImpl {

    @Override
    public String echo(String text) {
        return "DemoServiceImpl2:" + super.echo(text);
    }
}
