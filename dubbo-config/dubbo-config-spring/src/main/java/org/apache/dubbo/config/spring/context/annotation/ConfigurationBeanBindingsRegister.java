package org.apache.dubbo.config.spring.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * User: aini
 * Date: 2023/1/19 16:13
 */
public class ConfigurationBeanBindingsRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private ConfigurableEnvironment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
            importingClassMetadata.getAnnotationAttributes(EnableConfigurationBeanBindings.class.getName()));

        AnnotationAttributes[] annotationAttributes = attributes.getAnnotationArray("value");

        ConfigurationBeanBindingRegistrar registrar = new ConfigurationBeanBindingRegistrar();

        registrar.setEnvironment(environment);

        for (AnnotationAttributes element : annotationAttributes) {
            registrar.registerConfigurationBeanDefinitions(element, registry);
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        this.environment = (ConfigurableEnvironment) environment;
    }
}
