package org.apache.dubbo.rpc.model;

public class TestApplicationInitListener implements ApplicationInitListener {
    @Override
    public void init() {
        System.out.println("hihi");
    }
}
