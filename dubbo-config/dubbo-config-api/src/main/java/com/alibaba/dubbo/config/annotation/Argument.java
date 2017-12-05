package com.alibaba.dubbo.config.annotation;

import java.lang.annotation.*;

/**
 * Created by meixinbin on 2017/10/13.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Argument {

    int index() default -1;

    /**
     * 通过参数类型查找参数的index
     */
    Class<?> type() default void.class;

    /**
     * 参数是否为callback接口，如果为callback，服务提供方将生成反向代理，
     * 可以从服务提供方反向调用消费方，通常用于事件推送.
     * */
    boolean callback() default false;

}
