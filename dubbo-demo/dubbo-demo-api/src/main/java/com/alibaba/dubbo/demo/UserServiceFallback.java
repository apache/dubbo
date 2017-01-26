package com.alibaba.dubbo.demo;

import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by wuyu on 2017/1/11.
 */
public class UserServiceFallback implements UserService {

    @Override
    public String sayHello(@RequestParam("name") String name) {
        return "Hello "+name;
    }


}
