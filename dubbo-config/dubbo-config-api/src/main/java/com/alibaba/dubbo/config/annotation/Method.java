package com.alibaba.dubbo.config.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Method {

    //方法名
    String name() default "";

	// 方法使用线程数限制
    int  executes() default 0;
    
    // 最大并发调用
    int actives() default 0;
    
    // 负载均衡
    String loadbalance() default "";
    
    // 是否异步
    boolean async() default false;
    
    // 异步发送是否等待发送成功
    boolean sent() default false;
    
    // 服务接口的失败mock实现类名
    String mock() default "";
    
    // 服务接口的失败mock实现类名
    String cache() default "";
    
    // 服务接口的失败mock实现类名
    String validation() default "";
    
    // 是否重试
    boolean  retry() default true;

    // 是否为可靠异步
    boolean  reliable() default false;

    // 是否需要返回
    boolean  isReturn() default true;

    // 重试次数
    int  retries() default 3;
    
    // 是否需要开启stiky策略
    boolean sticky() default false;
    
    // 远程调用超时时间(毫秒)
    int timeout() default 3000;
    
    //异步调用回调实例
    String oninvoke() default "";
    
    //异步调用回调方法
    String  oninvokeMethod() default "";
    
    //异步调用回调实例
    String onreturn() default "";
    
    //异步调用回调方法
    String  onreturnMethod() default "";
    
    //异步调用异常回调实例
    String onthrow() default "";
    
    //异步调用异常回调方法
    String  onthrowMethod() default "";

    Argument[] arguments() default {};
}
