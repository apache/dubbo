package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.context.ConfigValidator;

@Activate
public class ConsumerConfigValidator implements ConfigValidator<ConsumerConfig> {

    @Override
    public void validate(ConsumerConfig config) {
        validateConsumerConfig(config);
    }

    public static void validateConsumerConfig(ConsumerConfig config) {
        //TODO
        if (config == null) {
        }
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return ConsumerConfigValidator.class.equals(configClass);
    }
}
