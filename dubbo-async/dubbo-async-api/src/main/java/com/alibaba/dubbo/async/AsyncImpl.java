package com.alibaba.dubbo.async;

import java.lang.annotation.*;

/**
 * Created by zhaohui.yu
 * 15/11/13
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface AsyncImpl {
    Class<?> value();
}