package com.alibaba.dubbo.demo;

import com.alibaba.dubbo.rpc.protocol.springmvc.annotation.Fallback;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by wuyu on 2017/1/10.
 */
@Fallback(fallback = UserServiceFallback.class)
public interface UserService {
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String id(@PathVariable(value = "id") String id);
}
