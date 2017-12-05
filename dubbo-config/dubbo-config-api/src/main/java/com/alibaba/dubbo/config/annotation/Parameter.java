package com.alibaba.dubbo.config.annotation;

import java.lang.annotation.*;

/**
 * Created by meixinbin on 2017/10/14.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Parameter {

    String key();

    String value();

}
