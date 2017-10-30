package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ReferenceBean} Builder
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 2.5.7
 */
class ReferenceBeanBuilder {

    private Reference reference;

    private Class<?> referenceClass;

    private ApplicationContext applicationContext;

    private ClassLoader classLoader;

    public ReferenceBeanBuilder setReference(Reference reference) {
        this.reference = reference;
        return this;
    }

    public ReferenceBeanBuilder setReferenceClass(Class<?> referenceClass) {
        Assert.notNull(referenceClass, "");
        this.referenceClass = referenceClass;
        return this;
    }

    public ReferenceBeanBuilder setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.classLoader = applicationContext.getClassLoader();
        return this;
    }

    private void setInterface(ReferenceBean referenceBean) {

        Class<?> interfaceClass = reference.interfaceClass();

        if (void.class.equals(interfaceClass)) {

            interfaceClass = null;

            String interfaceClassName = reference.interfaceName();

            if (StringUtils.hasText(interfaceClassName)) {
                if (ClassUtils.isPresent(interfaceClassName, classLoader)) {
                    interfaceClass = ClassUtils.resolveClassName(interfaceClassName, classLoader);
                }
            }

        }

        if (interfaceClass == null) {
            interfaceClass = referenceClass;
        }

        Assert.isTrue(interfaceClass.isInterface(),
                "The class of field or method that was annotated @Reference is not an interface!");

        referenceBean.setInterface(interfaceClass);

    }


    private void setRegistryConfigs(ReferenceBean<?> referenceBean) {

        String[] registryConfigNames = reference.registry();

        if (ObjectUtils.isEmpty(registryConfigNames)) {
            return;
        }

        List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>(registryConfigNames.length);

        for (String registryConfigName : registryConfigNames) {
            RegistryConfig registryConfig = applicationContext.getBean(registryConfigName, RegistryConfig.class);
            registryConfigs.add(registryConfig);
        }

        referenceBean.setRegistries(registryConfigs);

    }

    private void setConsumer(ReferenceBean<?> referenceBean) {

        String consumerBeanName = reference.consumer();

        if (StringUtils.hasText(consumerBeanName)) {
            ConsumerConfig consumerConfig = applicationContext.getBean(consumerBeanName, ConsumerConfig.class);
            referenceBean.setConsumer(consumerConfig);
        }

    }

    private void setMonitor(ReferenceBean<?> referenceBean) {

        String monitorBeanName = reference.monitor();

        if (StringUtils.hasText(monitorBeanName)) {
            MonitorConfig monitorConfig = applicationContext.getBean(monitorBeanName, MonitorConfig.class);
            referenceBean.setMonitor(monitorConfig);
        }

    }

    private void setApplication(ReferenceBean<?> referenceBean) {

        String applicationBeanName = reference.application();

        if (StringUtils.hasText(applicationBeanName)) {
            ApplicationConfig applicationConfig = applicationContext.getBean(applicationBeanName, ApplicationConfig.class);
            referenceBean.setApplication(applicationConfig);
        }

    }

    public void setModule(ReferenceBean<?> referenceBean) {

        String moduleBeanName = reference.module();

        if (StringUtils.hasText(moduleBeanName)) {
            ModuleConfig moduleConfig = applicationContext.getBean(moduleBeanName, ModuleConfig.class);
            referenceBean.setModule(moduleConfig);
        }

    }


    public ReferenceBean build() throws Exception {

        ReferenceBean<?> referenceBean = new ReferenceBean<Object>(reference);

        referenceBean.setApplicationContext(applicationContext);

        setInterface(referenceBean);

        setRegistryConfigs(referenceBean);

        setConsumer(referenceBean);

        setMonitor(referenceBean);

        setApplication(referenceBean);

        setModule(referenceBean);

        referenceBean.afterPropertiesSet();

        return referenceBean;

    }

    public static ReferenceBeanBuilder create() {
        return new ReferenceBeanBuilder();
    }

}
