package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.UserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by wuyu on 2017/1/10.
 */
public class UserServiceImpl implements UserService{
    @Override
    @ResponseBody
    public String id(@PathVariable(value = "id") String id) {
        return String.valueOf(Integer.parseInt(id));
    }
}
