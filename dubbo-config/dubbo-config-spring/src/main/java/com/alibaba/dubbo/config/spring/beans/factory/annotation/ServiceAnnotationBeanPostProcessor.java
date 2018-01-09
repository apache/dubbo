/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.config.spring.context.annotation.DubboClassPathBeanDefinitionScanner;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.*;

import java.util.*;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionReaderUtils.registerWithGeneratedName;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * {@link Service} Annotation
 * {@link BeanDefinitionRegistryPostProcessor Bean Definition Registry Post Processor}
 *
 * @since 2.5.8
 */
public class ServiceAnnotationBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware,
        ResourceLoaderAware, BeanClassLoaderAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<String> packagesToScan;

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    public ServiceAnnotationBeanPostProcessor(String... packagesToScan) {
        this(Arrays.asList(packagesToScan));
    }

    public ServiceAnnotationBeanPostProcessor(Collection<String> packagesToScan) {
        this(new LinkedHashSet<String>(packagesToScan));
    }

    public ServiceAnnotationBeanPostProcessor(Set<String> packagesToScan) {
        this.packagesToScan = packagesToScan;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        Set<String> resolvedPackagesToScan = resolvePackagesToScan(packagesToScan);

        registerServiceBeans(resolvedPackagesToScan, registry);

    }


    /**
     * Registers Beans whose classes was annotated {@link Service}
     *
     * @param packagesToScan The base packages to scan
     * @param registry       {@link BeanDefinitionRegistry}
     */
    private void registerServiceBeans(Set<String> packagesToScan, BeanDefinitionRegistry registry) {

        DubboClassPathBeanDefinitionScanner scanner =
                new DubboClassPathBeanDefinitionScanner(registry, environment, resourceLoader);

        BeanNameGenerator beanNameGenerator = resolveBeanNameGenerator(registry);

        scanner.setBeanNameGenerator(beanNameGenerator);

        scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));

        for (String packageToScan : packagesToScan) {

            Set<BeanDefinitionHolder> beanDefinitionHolders = scanner.doScan(packageToScan);

            if (CollectionUtils.isEmpty(beanDefinitionHolders)) {

                if (logger.isInfoEnabled()) {
                    logger.info("No Spring Bean annotating Dubbo's @Service was found in Spring BeanFactory , " +
                            "it maybe that some bean was also annotated @Component .");
                    logger.info("It will try to find all Bean types annotating Dubbo's @Service from all Bean Definitions");
                }

                beanDefinitionHolders = findServiceBeanDefinitionHolders(scanner, packageToScan, registry, beanNameGenerator);

            }

            if (!CollectionUtils.isEmpty(beanDefinitionHolders)) {

                for (BeanDefinitionHolder beanDefinitionHolder : beanDefinitionHolders) {
                    registerServiceBean(beanDefinitionHolder, registry);
                }

                if (logger.isInfoEnabled()) {
                    logger.info(beanDefinitionHolders.size() + " annotated Dubbo's @Service Components { " +
                            beanDefinitionHolders +
                            " } were scanned under package[" + packageToScan + "]");
                }

            } else {

                if (logger.isWarnEnabled()) {
                    logger.warn("No Spring Bean annotating Dubbo's @Service was found in Spring BeanFactory");
                }

            }

        }

    }

    /**
     * It'd better to use BeanNameGenerator instance that should reference
     * {@link ConfigurationClassPostProcessor#componentScanBeanNameGenerator},
     * thus it maybe a potential problem on bean name generation.
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @return {@link BeanNameGenerator} instance
     * @see SingletonBeanRegistry
     * @see AnnotationConfigUtils#CONFIGURATION_BEAN_NAME_GENERATOR
     * @see ConfigurationClassPostProcessor#processConfigBeanDefinitions
     * @since 2.5.8
     */
    private BeanNameGenerator resolveBeanNameGenerator(BeanDefinitionRegistry registry) {

        BeanNameGenerator beanNameGenerator = null;

        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry singletonBeanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGenerator = (BeanNameGenerator) singletonBeanRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
        }

        if (beanNameGenerator == null) {

            if (logger.isInfoEnabled()) {

                logger.info("BeanNameGenerator bean can't be found in BeanFactory with name ["
                        + CONFIGURATION_BEAN_NAME_GENERATOR + "]");
                logger.info("BeanNameGenerator will be a instance of " +
                        AnnotationBeanNameGenerator.class.getName() +
                        " , it maybe a potential problem on bean name generation.");
            }

            beanNameGenerator = new AnnotationBeanNameGenerator();

        }

        return beanNameGenerator;

    }

    /**
     * Finds a {@link Set} of {@link BeanDefinitionHolder BeanDefinitionHolders} whose bean type annotated
     * {@link Service} Annotation.
     *
     * @param scanner       {@link ClassPathBeanDefinitionScanner}
     * @param packageToScan pachage to scan
     * @param registry      {@link BeanDefinitionRegistry}
     * @return non-null
     * @since 2.5.8
     */
    private Set<BeanDefinitionHolder> findServiceBeanDefinitionHolders(
            ClassPathBeanDefinitionScanner scanner, String packageToScan, BeanDefinitionRegistry registry,
            BeanNameGenerator beanNameGenerator) {

        Set<BeanDefinition> beanDefinitions = scanner.findCandidateComponents(packageToScan);

        Set<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<BeanDefinitionHolder>(beanDefinitions.size());

        for (BeanDefinition beanDefinition : beanDefinitions) {

            String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);

        }

        return beanDefinitionHolders;

    }

    /**
     * Registers {@link ServiceBean} from new annotated {@link Service} {@link BeanDefinition}
     *
     * @param beanDefinitionHolder
     * @param registry
     * @see ServiceBean
     * @see BeanDefinition
     */
    private void registerServiceBean(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry registry) {

        Class<?> beanClass = resolveClass(beanDefinitionHolder);

        Service service = findAnnotation(beanClass, Service.class);

        Class<?> interfaceClass = resolveServiceInterfaceClass(beanClass, service);

        String beanName = beanDefinitionHolder.getBeanName();

        AbstractBeanDefinition serviceBeanDefinition = buildServiceBeanDefinition(service, interfaceClass, beanName);

        registerWithGeneratedName(serviceBeanDefinition, registry);

    }

    private Class<?> resolveServiceInterfaceClass(Class<?> annotatedServiceBeanClass, Service service) {

        Class<?> interfaceClass = service.interfaceClass();

        if (void.class.equals(interfaceClass)) {

            interfaceClass = null;

            String interfaceClassName = service.interfaceName();

            if (StringUtils.hasText(interfaceClassName)) {
                if (ClassUtils.isPresent(interfaceClassName, classLoader)) {
                    interfaceClass = resolveClassName(interfaceClassName, classLoader);
                }
            }

        }

        if (interfaceClass == null) {

            Class<?>[] allInterfaces = annotatedServiceBeanClass.getInterfaces();

            if (allInterfaces.length > 0) {
                interfaceClass = allInterfaces[0];
            }

        }

        Assert.notNull(interfaceClass,
                "@Service interfaceClass() or interfaceName() or interface class must be present!");

        Assert.isTrue(interfaceClass.isInterface(),
                "The type that was annotated @Service is not an interface!");

        return interfaceClass;
    }

    private Class<?> resolveClass(BeanDefinitionHolder beanDefinitionHolder) {

        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();

        return resolveClass(beanDefinition);

    }

    private Class<?> resolveClass(BeanDefinition beanDefinition) {

        String beanClassName = beanDefinition.getBeanClassName();

        return resolveClassName(beanClassName, classLoader);

    }

    private Set<String> resolvePackagesToScan(Set<String> packagesToScan) {
        Set<String> resolvedPackagesToScan = new LinkedHashSet<String>(packagesToScan.size());
        for (String packageToScan : packagesToScan) {
            String resolvedPackageToScan = environment.resolvePlaceholders(packageToScan);
            resolvedPackagesToScan.add(resolvedPackageToScan);
        }
        return resolvedPackagesToScan;
    }

    private AbstractBeanDefinition buildServiceBeanDefinition(Service service, Class<?> interfaceClass,
                                                              String annotatedServiceBeanName) {

        BeanDefinitionBuilder builder = rootBeanDefinition(ServiceBean.class)
                .addConstructorArgValue(service)
                // References "ref" property to annotated-@Service Bean
                .addPropertyReference("ref", annotatedServiceBeanName)
                .addPropertyValue("interfaceClass", interfaceClass);

        /**
         * Add {@link com.alibaba.dubbo.config.ProviderConfig} Bean reference
         */
        String providerConfigBeanName = service.provider();
        if (StringUtils.hasText(providerConfigBeanName)) {
            addPropertyReference(builder, "provider", providerConfigBeanName);
        }

        /**
         * Add {@link com.alibaba.dubbo.config.MonitorConfig} Bean reference
         */
        String monitorConfigBeanName = service.monitor();
        if (StringUtils.hasText(monitorConfigBeanName)) {
            addPropertyReference(builder, "monitor", monitorConfigBeanName);
        }

        /**
         * Add {@link com.alibaba.dubbo.config.ApplicationConfig} Bean reference
         */
        String applicationConfigBeanName = service.application();
        if (StringUtils.hasText(applicationConfigBeanName)) {
            addPropertyReference(builder, "application", applicationConfigBeanName);
        }

        /**
         * Add {@link com.alibaba.dubbo.config.ModuleConfig} Bean reference
         */
        String moduleConfigBeanName = service.module();
        if (StringUtils.hasText(moduleConfigBeanName)) {
            addPropertyReference(builder, "module", moduleConfigBeanName);
        }


        /**
         * Add {@link com.alibaba.dubbo.config.RegistryConfig} Bean reference
         */
        String[] registryConfigBeanNames = service.registry();

        List<RuntimeBeanReference> registryRuntimeBeanReferences = toRuntimeBeanReferences(registryConfigBeanNames);

        if (!registryRuntimeBeanReferences.isEmpty()) {
            builder.addPropertyValue("registries", registryRuntimeBeanReferences);
        }

        /**
         * Add {@link com.alibaba.dubbo.config.ProtocolConfig} Bean reference
         */
        String[] protocolConfigBeanNames = service.protocol();

        List<RuntimeBeanReference> protocolRuntimeBeanReferences = toRuntimeBeanReferences(protocolConfigBeanNames);

        if (!registryRuntimeBeanReferences.isEmpty()) {
            builder.addPropertyValue("protocols", protocolRuntimeBeanReferences);
        }

        return builder.getBeanDefinition();

    }


    private ManagedList<RuntimeBeanReference> toRuntimeBeanReferences(String... beanNames) {

        ManagedList<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<RuntimeBeanReference>();

        if (!ObjectUtils.isEmpty(beanNames)) {

            for (String beanName : beanNames) {

                String resolvedBeanName = environment.resolvePlaceholders(beanName);

                runtimeBeanReferences.add(new RuntimeBeanReference(resolvedBeanName));
            }

        }

        return runtimeBeanReferences;

    }

    private void addPropertyReference(BeanDefinitionBuilder builder, String propertyName, String beanName) {
        String resolvedBeanName = environment.resolvePlaceholders(beanName);
        builder.addPropertyReference(propertyName, resolvedBeanName);
    }


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
