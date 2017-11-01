package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import com.alibaba.dubbo.config.spring.beans.factory.annotation.ServiceAnnotationBeanPostProcessor;
import com.alibaba.dubbo.config.spring.util.BeanRegistrar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.*;

import static com.alibaba.dubbo.config.spring.util.BeanRegistrar.registerInfrastructureBean;

/**
 * Dubbo {@link DubboComponentScan} Bean Registrar
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Service
 * @see DubboComponentScan
 * @see ImportBeanDefinitionRegistrar
 * @since 2.5.7
 */
public class DubboComponentScanRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private final Log logger = LogFactory.getLog(getClass());

    private ResourceLoader resourceLoader;

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        registerServiceBeans(importingClassMetadata, registry);

        registerServiceAnnotationBeanPostProcessor(registry);

        registerReferenceAnnotationBeanPostProcessor(registry);

    }


    /**
     * Registers Beans whose classes was annotated {@link Service}
     *
     * @param importingClassMetadata {@link AnnotationMetadata}
     * @param registry               {@link BeanDefinitionRegistry}
     */
    private void registerServiceBeans(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);

        ClassPathBeanDefinitionScanner classPathBeanDefinitionScanner =
                new ClassPathBeanDefinitionScanner(registry, false);

        classPathBeanDefinitionScanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));

        classPathBeanDefinitionScanner.setResourceLoader(resourceLoader);
        classPathBeanDefinitionScanner.setEnvironment(environment);

        Set<String> beanNames = new HashSet<String>(Arrays.asList(registry.getBeanDefinitionNames()));

        for (String packageToScan : packagesToScan) {
            int serviceBeansCount = classPathBeanDefinitionScanner.scan(packageToScan);

            String[] names = registry.getBeanDefinitionNames();

            Set<String> newBeanNames = new HashSet<String>(Arrays.asList(names));
            newBeanNames.removeAll(beanNames);

            beanNames.addAll(Arrays.asList(registry.getBeanDefinitionNames()));

            if (logger.isInfoEnabled()) {
                logger.info(serviceBeansCount + " annotated @Service Components{ bean names : " + newBeanNames +
                        "} were scanned under package[" + packageToScan + "]");
            }
        }

    }

    /**
     * Registers {@link ServiceAnnotationBeanPostProcessor} into {@link BeanFactory}
     *
     * @param registry {@link BeanDefinitionRegistry}
     */
    private void registerServiceAnnotationBeanPostProcessor(BeanDefinitionRegistry registry) {

        registerInfrastructureBean(registry, ServiceAnnotationBeanPostProcessor.BEAN_NAME,
                ServiceAnnotationBeanPostProcessor.class);


    }

    /**
     * Registers {@link ReferenceAnnotationBeanPostProcessor} into {@link BeanFactory}
     *
     * @param registry {@link BeanDefinitionRegistry}
     */
    private void registerReferenceAnnotationBeanPostProcessor(BeanDefinitionRegistry registry) {

        // Register @Reference Annotation Bean Processor
        BeanRegistrar.registerInfrastructureBean(registry,
                ReferenceAnnotationBeanPostProcessor.BEAN_NAME, ReferenceAnnotationBeanPostProcessor.class);

    }

    private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(DubboComponentScan.class.getName()));
        String[] basePackages = attributes.getStringArray("basePackages");
        Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
        Set<String> packagesToScan = new LinkedHashSet<String>();
        packagesToScan.addAll(Arrays.asList(basePackages));
        for (Class<?> basePackageClass : basePackageClasses) {
            packagesToScan.add(ClassUtils.getPackageName(basePackageClass));
        }
        if (packagesToScan.isEmpty()) {
            return Collections.singleton(ClassUtils.getPackageName(metadata.getClassName()));
        }
        return packagesToScan;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
