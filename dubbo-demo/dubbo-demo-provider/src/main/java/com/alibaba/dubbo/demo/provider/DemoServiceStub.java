package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.ParamCallback;
import com.alibaba.dubbo.demo.entity.User;
import com.alibaba.dubbo.demo.enumx.Sex;

import java.util.Collection;

public class DemoServiceStub implements DemoService {

    private DemoService demoService;

    public DemoServiceStub(DemoService demoService) {
        this.demoService = demoService;
    }

    @Override
    public String sayHello(String name) {
        return null;
    }

    @Override
    public void bye(Object o) {
        System.out.println("o");
    }

    @Override
    public void callbackParam(String msg, ParamCallback callback) {
        System.out.println("o");
    }

    @Override
    public String say01(String msg) {
        return null;
    }

    @Override
    public String[] say02() {
        return new String[0];
    }

    @Override
    public void say03() {
        System.out.println("o");
    }

    @Override
    public Void say04() {
        return null;
    }

    @Override
    public void save(User user) {

    }

    @Override
    public void update(User user) {

    }

    @Override
    public void delete(User user, Boolean vip) {

    }

    @Override
    public void saves(Collection<User> users) {

    }

    @Override
    public void saves(User[] users) {

    }

    @Override
    public void demo(String name, String password, User user) {

    }

    @Override
    public void demo(Sex sex) {

    }

    @Override
    public void hello(String name) {

    }

    @Override
    public void hello01(String name) {

    }

    @Override
    public void hello02(String name) {

    }

}
