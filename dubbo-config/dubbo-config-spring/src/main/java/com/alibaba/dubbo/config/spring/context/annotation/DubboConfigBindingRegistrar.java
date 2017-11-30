package com.alibaba.dubbo.config.spring.context.annotation;

import com.alibaba.dubbo.config.AbstractConfig;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.alibaba.dubbo.config.spring.util.PropertySourcesUtils.getSubProperties;

/**
 * {@link AbstractConfig Dubbo Config} binding Bean registrar
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EnableDubboConfigBinding
 * @since 2.5.8
 */
public class DubboConfigBindingRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private ConfigurableEnvironment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnableDubboConfigBinding.class.getName()));

        registerBeanDefinitions(attributes, registry);

    }

    protected void registerBeanDefinitions(AnnotationAttributes attributes, BeanDefinitionRegistry registry) {

        String prefix = environment.resolvePlaceholders(attributes.getString("prefix"));

        Class<? extends AbstractConfig> configClass = attributes.getClass("type");

        boolean multiple = attributes.getBoolean("multiple");

        registerDubboConfigBeans(prefix, configClass, multiple, registry);

    }

    private void registerDubboConfigBeans(String prefix,
                                          Class<? extends AbstractConfig> configClass,
                                          boolean multiple,
                                          BeanDefinitionRegistry registry) {

        PropertySources propertySources = environment.getPropertySources();

        Map<String, String> properties = getSubProperties(propertySources, prefix);

        Set<String> beanNames = multiple ? resolveMultipleBeanNames(prefix, properties) :
                Collections.singleton(resolveSingleBeanName(configClass, properties, registry));

        for (String beanName : beanNames) {

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(configClass);

            AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

            beanDefinition.setPropertyValues(resolveBeanPropertyValues(beanName, multiple, properties));

            registry.registerBeanDefinition(beanName, beanDefinition);

        }

    }

    private MutablePropertyValues resolveBeanPropertyValues(String beanName, boolean multiple,
                                                            Map<String, String> properties) {

        MutablePropertyValues propertyValues = new MutablePropertyValues();

        if (multiple) { // For Multiple Beans

            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addFirst(new MapPropertySource(beanName, new TreeMap<String, Object>(properties)));

            Map<String, String> subProperties = getSubProperties(propertySources, beanName);

            propertyValues.addPropertyValues(subProperties);


        } else { // For Single Bean

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                if (!propertyName.contains(".")) { // ignore property name with "."
                    propertyValues.addPropertyValue(propertyName, entry.getValue());
                }
            }

        }

        return propertyValues;

    }

    @Override
    public void setEnvironment(Environment environment) {

        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);

        this.environment = (ConfigurableEnvironment) environment;

    }

    private Set<String> resolveMultipleBeanNames(String prefix, Map<String, String> properties) {

        Set<String> beanNames = new LinkedHashSet<String>();

        for (String propertyName : properties.keySet()) {

            int index = propertyName.indexOf(".");

            if (index > 0) {

                String beanName = propertyName.substring(0, index);

                beanNames.add(beanName);
            }

        }

        return beanNames;

    }

    private String resolveSingleBeanName(Class<? extends AbstractConfig> configClass, Map<String, String> properties,
                                         BeanDefinitionRegistry registry) {

        String beanName = properties.get("id");

        if (!StringUtils.hasText(beanName)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(configClass);
            beanName = BeanDefinitionReaderUtils.generateBeanName(builder.getRawBeanDefinition(), registry);
        }

        return beanName;

    }

}
