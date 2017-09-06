package com.alibaba.dubbo.async;

import java.io.Serializable;

/**
 * Created by zhaohui.yu
 * 15/11/13
 */
public interface AsyncContext<T extends Serializable> {
    void commit();

    void commit(T result);

    void fail(Throwable t);
}

