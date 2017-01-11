package com.alibaba.dubbo.demo;

import org.springframework.web.bind.annotation.PathVariable;

/**
 * Created by wuyu on 2017/1/11.
 */
public class UserServiceFallback implements UserService {
    @Override
    public String id(@PathVariable(value = "id") String id) {
        return "1";
    }


}
