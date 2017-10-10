package com.alibaba.dubbo.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法分组验证注解
 * @author: zhangyinyue
 * @Createdate: 2017年10月10日 16:34
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodValidated {
    Class<?>[] value() default {};
}
