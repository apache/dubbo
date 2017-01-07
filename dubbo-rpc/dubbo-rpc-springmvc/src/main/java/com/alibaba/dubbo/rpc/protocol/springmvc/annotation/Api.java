package com.alibaba.dubbo.rpc.protocol.springmvc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Api {

    String value();

    int maxConnTotal() default 20;

    int timeout() default 2000;

    int retry() default 5;

    boolean keepAlive() default false;

}
