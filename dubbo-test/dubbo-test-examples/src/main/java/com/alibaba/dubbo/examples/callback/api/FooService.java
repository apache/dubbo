package com.alibaba.dubbo.examples.callback.api;

/**
 * Created by tanhua on 16/4/15.
 */
public interface FooService {

    void asyncCallInFoo(String key, CallbackListener listener);
}
