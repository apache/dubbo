package com.alibaba.dubbo.examples.generic.api;

public interface IService<P, V> {
    V get(P params);
}
