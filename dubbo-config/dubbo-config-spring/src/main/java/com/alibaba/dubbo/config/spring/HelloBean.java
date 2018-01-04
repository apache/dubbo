package com.alibaba.dubbo.config.spring;

import org.springframework.beans.factory.InitializingBean;


public class HelloBean implements InitializingBean{


    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("hello world yyy");
    }
}
