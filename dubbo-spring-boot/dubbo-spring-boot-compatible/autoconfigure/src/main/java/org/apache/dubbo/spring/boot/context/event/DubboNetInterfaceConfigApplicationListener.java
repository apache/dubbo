package org.apache.dubbo.spring.boot.context.event;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @since 3.2
 */
public class DubboNetInterfaceConfigApplicationListener implements ApplicationListener<ApplicationPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent applicationPreparedEvent) {
        ConfigurableEnvironment environment = applicationPreparedEvent.getApplicationContext()
                .getEnvironment();
        String preferredNetworkInterface = System.getProperty(CommonConstants.DUBBO_PREFERRED_NETWORK_INTERFACE);
        if (StringUtils.isBlank(preferredNetworkInterface)) {
            preferredNetworkInterface = environment.getProperty(CommonConstants.DUBBO_PREFERRED_NETWORK_INTERFACE);
            if (StringUtils.isNotBlank(preferredNetworkInterface)) {
                System.setProperty(CommonConstants.DUBBO_PREFERRED_NETWORK_INTERFACE, preferredNetworkInterface);
            }
        }
        String ignoredNetworkInterface = System.getProperty(CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE);
        if (StringUtils.isBlank(ignoredNetworkInterface)) {
            ignoredNetworkInterface = environment.getProperty(CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE);
            if (StringUtils.isNotBlank(ignoredNetworkInterface)) {
                System.setProperty(CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE, ignoredNetworkInterface);
            }
        }
    }
}

