package com.alibaba.dubbo.rpc.protocol.springmvc.exception;

import feign.FeignException;

/**
 * Created by wuyu on 2016/9/15.
 */
public class SpringMvcException extends FeignException{

    protected SpringMvcException(int status, String message) {
        super(status, message);
    }
}
