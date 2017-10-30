package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

import java.lang.annotation.Annotation;

/**
 * Dubbo {@link Annotation} Bean Processor Registrar
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.7
 */
public class AnnotationBeanProcessorRegistrar {

    private static final String REFERENCE_ANNOTATION_BEAN_POST_PROCESSOR_BEAN_NAME = "referenceAnnotationBeanPostProcessor";

    /**
     * Register {@link BeanDefinitionRegistry}
     *
     * @param beanDefinitionRegistry {@link BeanDefinitionRegistry}
     */
    public void register(BeanDefinitionRegistry beanDefinitionRegistry) {

        registerBeanPostProcessor(beanDefinitionRegistry,
                ReferenceAnnotationBeanPostProcessor.class,
                REFERENCE_ANNOTATION_BEAN_POST_PROCESSOR_BEAN_NAME);

    }

    /**
     * Register {@link BeanPostProcessor}
     *
     * @param beanDefinitionRegistry {@link BeanDefinitionRegistry}
     * @param beanPostProcessorClass the class of {@link BeanPostProcessor}
     * @param beanName               the name of bean
     */
    private void registerBeanPostProcessor(BeanDefinitionRegistry beanDefinitionRegistry,
                                           Class<?> beanPostProcessorClass,
                                           String beanName) {

        if (!beanDefinitionRegistry.containsBeanDefinition(beanName)) {
            RootBeanDefinition beanDefinition = new RootBeanDefinition(beanPostProcessorClass);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition);
        }

    }

}
