package com.alibaba.dubbo.rpc;

import java.lang.annotation.*;

/**
 * Created by wuyu on 2017/1/13.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FallBack {

    //熔断类
    Class<?> value() default void.class;

    //线程池大小
    int coreSize() default 20;

    //10秒钟内至少19此请求失败,启用熔断
    int circuitBreakerRequestVolumeThreshold() default 20;

    //熔断器中断请求30秒后会进入半打开状态,放部分流量过去重试
    int circuitBreakerSleepWindowInMilliseconds() default 30000;

    //错误率达到50开启熔断保护
    int withCircuitBreakerErrorThresholdPercentage() default 50;

    //使用dubbo的超时，禁用这里的超时
    boolean withExecutionTimeoutEnabled() default false;
}
