package com.alibaba.dubbo.config.spring.context.annotation;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

import java.util.Set;

import static org.springframework.context.annotation.AnnotationConfigUtils.registerAnnotationConfigProcessors;

/**
 * Dubbo {@link ClassPathBeanDefinitionScanner} that exposes some methods to be public.
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see #doScan(String...)
 * @see #registerDefaultFilters()
 * @since 2.5.7
 */
public class DubboClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {


    public DubboClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters, Environment environment,
                                               ResourceLoader resourceLoader) {

        super(registry, useDefaultFilters, environment, resourceLoader);

        registerAnnotationConfigProcessors(registry);

    }

    public DubboClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, Environment environment,
                                               ResourceLoader resourceLoader) {

        this(registry, false, environment, resourceLoader);

    }

    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        return super.doScan(basePackages);
    }


}
