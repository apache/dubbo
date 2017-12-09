package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.ApplicationConfig;

import java.lang.annotation.*;

/**
 * Equals {@link EnableDubboConfigBinding} for {@link ApplicationConfig} with "dubbo.application"
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.8
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableDubboConfigBinding(prefix = "dubbo.application", type = ApplicationConfig.class)
public @interface EnableApplicationConfig {
}
