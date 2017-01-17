package com.alibaba.dubbo.rpc.protocol.springmvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Api {

    String value() default "";

    int maxConnTotal() default 20;

    int timeout() default 2000;

    int retry() default 5;

    boolean keepAlive() default false;

    Class<?> fallback() default void.class;


}
