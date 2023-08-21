package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.context.ConfigValidator;

@Activate
public class ConsumerConfigValidator implements ConfigValidator<ConsumerConfig> {

    public static void validateConsumerConfig(ConsumerConfig config) {
        //TODO
        if (config == null) {
        }
    }

    @Override
    public void validate(ConsumerConfig config) {
        validateConsumerConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ConsumerConfig.class.equals(configClass);
    }
}
