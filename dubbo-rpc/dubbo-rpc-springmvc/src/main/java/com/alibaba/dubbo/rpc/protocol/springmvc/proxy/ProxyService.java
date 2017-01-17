package com.alibaba.dubbo.rpc.protocol.springmvc.proxy;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by wuyu on 2016/7/14.
 */
public interface ProxyService {

    @RequestMapping(value = "/", method = {RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
    public Object proxy(@RequestBody GenericServiceConfig config);

}
