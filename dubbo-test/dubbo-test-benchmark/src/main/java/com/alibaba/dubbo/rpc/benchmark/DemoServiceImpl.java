package com.alibaba.dubbo.rpc.benchmark;


public class DemoServiceImpl implements DemoService {
    ResponseObject responseObject = new ResponseObject(100);

    public Object sendRequest(Object request) {
        return request;
    }
}
