package org.apache.dubbo.config.spring.util;

import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Properties;

public class SpringConfigUtils {

    public static void setDubboSpringConfig(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }

        ApplicationConfig applicationConfig = applicationContext.getBean(ApplicationConfig.class);
        if (applicationConfig == null || applicationConfig.getParameters() == null) {
            return;
        }

        Properties properties = ConfigUtils.getProperties();
        for (Map.Entry<String, String> entry: applicationConfig.getParameters().entrySet()) {
            if (entry.getValue() != null && entry.getValue().trim().length() > 0) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
