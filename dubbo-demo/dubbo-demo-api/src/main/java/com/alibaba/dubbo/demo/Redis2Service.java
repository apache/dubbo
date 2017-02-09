package com.alibaba.dubbo.demo;

/**
 * Created by wuyu on 2017/2/7.
 */
public interface Redis2Service {
    public String sayHello(String name);

    public Integer sum(int i, int j);

    public User getById(String id);
}
