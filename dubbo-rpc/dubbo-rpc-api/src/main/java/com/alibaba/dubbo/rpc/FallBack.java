package com.alibaba.dubbo.rpc;

import java.lang.annotation.*;

/**
 * Created by wuyu on 2017/1/13.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FallBack {
    Class<?> value() default void.class;
}
