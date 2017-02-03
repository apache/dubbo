package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.HproseService;

/**
 * Created by wuyu on 2017/2/3.
 */
public class HproseServiceImpl implements HproseService {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}
