package org.apache.dubbo.config.spring.context.config;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;

import java.util.Map;

/**
 * User: aini
 * Date: 2023/1/19 16:32
 */
public class DefaultConfigurationBeanBinder implements ConfigurationBeanBinder {

    @Override
    public void bind(Map<String, Object> configurationProperties, boolean ignoreUnknownFields,
                     boolean ignoreInvalidFields, Object configurationBean) {
        DataBinder dataBinder = new DataBinder(configurationBean);
        // Set ignored*
        dataBinder.setIgnoreInvalidFields(ignoreUnknownFields);
        dataBinder.setIgnoreUnknownFields(ignoreInvalidFields);
        // Get properties under specified prefix from PropertySources
        // Convert Map to MutablePropertyValues
        MutablePropertyValues propertyValues = new MutablePropertyValues(configurationProperties);
        // Bind
        dataBinder.bind(propertyValues);
    }

}
