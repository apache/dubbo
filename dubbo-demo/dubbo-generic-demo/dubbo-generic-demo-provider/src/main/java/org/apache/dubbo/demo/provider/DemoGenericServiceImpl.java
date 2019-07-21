package org.apache.dubbo.demo.provider;

public class DemoGenericServiceImpl implements DemoGenericService {
    @Override
    public ResponseDemo forGeneric(RequestDemo requestDemo) {
        ResponseDemo responseDemo =  new ResponseDemo();
        responseDemo.setMsg(requestDemo.getName());
        responseDemo.setDesc(requestDemo.getDesc());
        responseDemo.setExtra(requestDemo.getExtra());
        responseDemo.setStatusCode(1);
        return responseDemo;
    }
}
