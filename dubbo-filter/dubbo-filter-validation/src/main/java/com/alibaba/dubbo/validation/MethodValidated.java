package com.alibaba.dubbo.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法分组验证注解
 * <p>使用场景：当调用某个方法时，需要检查多个分组，可以在接口方法上加上该注解</p><br>
 * 用法:<pre>   @MethodValidated({Save.class, Update.class})
 *  void relatedQuery(ValidationParameter parameter);</pre>
 *  在接口方法上增加注解,表示relatedQuery这个方法需要同时检查Save和Update这两个分组
 *
 * @author: zhangyinyue
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodValidated {
    Class<?>[] value() default {};
}
