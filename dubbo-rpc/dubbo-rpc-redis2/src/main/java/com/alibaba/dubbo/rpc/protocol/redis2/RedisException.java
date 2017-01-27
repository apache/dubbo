package com.alibaba.dubbo.rpc.protocol.redis2;

/**
 * Created by wuyu on 2017/1/17.
 */
public class RedisException extends RuntimeException {
    public RedisException(String message) {
        super(message);
    }
}
