package com.alibaba.dubbo.demo;

/**
 * Created by wuyu on 2017/2/7.
 */
public interface Redis2Service {
    public String sayHello(String name);

    public Long sum(Long i, Long j);

    public User getById(String id);

    public void insert(String id);

    public String returnNull();
}
