package org.apache.dubbo.config.spring.context.config;

import org.apache.dubbo.config.spring.context.annotation.EnableConfigurationBeanBinding;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * User: aini
 * Date: 2023/1/19 16:23
 */
public interface ConfigurationBeanBinder {


    /**
     * Bind the properties in the {@link Environment} to Configuration bean under specified prefix.
     *
     * @param configurationProperties The configuration properties
     * @param ignoreUnknownFields     whether to ignore unknown fields, the value is come
     *                                from the attribute of {@link EnableConfigurationBeanBinding#ignoreUnknownFields()}
     * @param ignoreInvalidFields     whether to ignore invalid fields, the value is come
     *                                from the attribute of {@link EnableConfigurationBeanBinding#ignoreInvalidFields()}
     * @param configurationBean       the bean of configuration
     */
    void bind(Map<String, Object> configurationProperties, boolean ignoreUnknownFields, boolean ignoreInvalidFields,
              Object configurationBean);
}
