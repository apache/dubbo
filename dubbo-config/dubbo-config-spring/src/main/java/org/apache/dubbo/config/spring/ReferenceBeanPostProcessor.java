package org.apache.dubbo.config.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Post-processing ReferenceBean
 */
public class ReferenceBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    public static String BEAN_NAME = "dubboReferenceBeanPostProcessor";

    private ApplicationContext applicationContext;

    private ReferenceBeanManager referenceBeanManager;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ReferenceBean) {
            ReferenceBean referenceBean = (ReferenceBean) bean;
            referenceBeanManager.addReference(referenceBean);
        }
        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        referenceBeanManager = applicationContext.getBean(ReferenceBeanManager.BEAN_NAME, ReferenceBeanManager.class);
    }
}
