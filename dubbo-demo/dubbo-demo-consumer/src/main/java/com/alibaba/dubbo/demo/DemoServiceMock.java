package com.alibaba.dubbo.demo;

/**
 * @author luokai
 * @date 2018/6/25
 */
public class DemoServiceMock implements DemoService {
    @Override
    public String sayHello(String name) {
        System.out.println("调用mock数据");
        return null;
    }

    @Override
    public SimpleObject changeSimple(SimpleObject simpleObject) {
        return null;
    }

    @Override
    public void throwsEx() {

    }
}
