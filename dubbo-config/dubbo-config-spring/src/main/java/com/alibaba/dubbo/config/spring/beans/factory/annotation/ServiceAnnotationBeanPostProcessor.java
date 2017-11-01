package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

/**
 * Dubbo {@link Service} {@link Annotation} {@link BeanPostProcessor}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.7
 */
public class ServiceAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
        implements ApplicationContextAware, BeanClassLoaderAware, PriorityOrdered, DisposableBean {

    /**
     * The bean name of {@link ServiceAnnotationBeanPostProcessor}
     */
    public static final String BEAN_NAME = "serviceAnnotationBeanPostProcessor";

    private final Log logger = LogFactory.getLog(getClass());

    private final List<ServiceBean<?>> serviceBeans = new LinkedList<ServiceBean<?>>();

    private ApplicationContext applicationContext;

    private ClassLoader classLoader;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        Class<?> beanClass = bean.getClass();

        // To find @Service Bean
        Service service = AnnotationUtils.findAnnotation(beanClass, Service.class);

        if (service != null) {

            ServiceBeanBuilder builder = ServiceBeanBuilder.create(service, classLoader, applicationContext)
                    .interfaceClass(beanClass)
                    .bean(bean);

            try {
                ServiceBean<Object> serviceBean = builder.build();
                serviceBean.export();

                if (logger.isInfoEnabled()) {
                    logger.info(serviceBean + " was exported!");
                }

                serviceBeans.add(serviceBean);
            } catch (Exception e) {
                throw new BeanInitializationException(e.getMessage(), e);
            }

        }

        return bean;

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        this.applicationContext = applicationContext;

    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {

        this.classLoader = classLoader;

    }

    @Override
    public void destroy() throws Exception {

        for (ServiceBean serviceBean : serviceBeans) {
            if (logger.isInfoEnabled()) {
                logger.info(serviceBean + " was destroying!");
            }
            serviceBean.destroy();

        }

        serviceBeans.clear();

        if (logger.isInfoEnabled()) {
            logger.info(getClass() + " was destroying!");
        }

    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE + 1;
    }
}
