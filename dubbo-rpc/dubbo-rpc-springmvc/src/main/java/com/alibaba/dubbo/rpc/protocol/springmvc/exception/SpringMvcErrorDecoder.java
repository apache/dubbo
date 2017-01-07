package com.alibaba.dubbo.rpc.protocol.springmvc.exception;

import com.alibaba.dubbo.common.utils.IOUtils;
import com.alibaba.fastjson.JSON;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Created by wuyu on 2016/9/15.
 */
public class SpringMvcErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        try {
            String read = IOUtils.read(response.body().asReader());
            String message = JSON.parseObject(read).getString("message");
            return new SpringMvcException(status, message);
        } catch (Exception e) {
        }
        return new SpringMvcException(status, response.toString());
    }
}
