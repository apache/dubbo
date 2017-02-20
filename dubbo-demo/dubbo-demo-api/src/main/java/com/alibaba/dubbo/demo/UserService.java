package com.alibaba.dubbo.demo;

import com.alibaba.dubbo.rpc.protocol.springmvc.annotation.Api;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Created by wuyu on 2017/1/10.
 */
@Api(fallback = UserServiceFallback.class)
@RequestMapping(value = "/user")
public interface UserService {

    @RequestMapping(value = "/sayHello", method = RequestMethod.GET)
    public String sayHello(@RequestParam("name") String name);



}
