package org.apache.dubbo.config.spring.context.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: aini
 * Date: 2023/1/19 16:09
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ConfigurationBeanBindingsRegister.class)
public @interface EnableConfigurationBeanBindings {


    /**
     * @return the array of {@link EnableConfigurationBeanBinding EnableConfigurationBeanBindings}
     */
    EnableConfigurationBeanBinding[] value();
}
