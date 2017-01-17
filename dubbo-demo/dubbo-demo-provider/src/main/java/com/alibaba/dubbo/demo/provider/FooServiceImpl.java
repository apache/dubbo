package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.FooService;
import org.apache.thrift.TException;

/**
 * Created by wuyu on 2017/1/17.
 */
public class FooServiceImpl implements FooService.Iface {
    @Override
    public String sayHello(String name) throws TException {
        return "Hello " + name;
    }
}
