package org.apache.dubbo.spring.boot.context;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.spring.boot.context.event.DubboNetInterfaceConfigApplicationListener;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_NETWORK_IGNORED_INTERFACE;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PREFERRED_NETWORK_INTERFACE;

/**
 * @since 3.2
 */
public class DubboNetInterfaceConfigApplicationContextInitializerTest {

    private static final String USE_NETWORK_INTERFACE_NAME = "eth0";

    private static final String IGNORED_NETWORK_INTERFACE_NAME = "eth1";

    @Test
    public void testInitialize() {
        SpringApplication application =
                new SpringApplication(DubboNetInterfaceConfigApplicationContextInitializerTest.class);
        application.addListeners(new DubboNetInterfaceConfigApplicationListener());
        application.addListeners(new NetworkInterfaceApplicationListener());
        application.run();
        String preferredNetworkInterface = System.getProperty(DUBBO_PREFERRED_NETWORK_INTERFACE);
        String ignoredNetworkInterface = System.getProperty(DUBBO_NETWORK_IGNORED_INTERFACE);
        Assert.assertEquals(USE_NETWORK_INTERFACE_NAME, preferredNetworkInterface);
        Assert.assertEquals(IGNORED_NETWORK_INTERFACE_NAME, ignoredNetworkInterface);

    }

    static class NetworkInterfaceApplicationListener
            implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

        @Override
        public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
            ConfigurableEnvironment environment = applicationEnvironmentPreparedEvent.getEnvironment();
            MutablePropertySources propertySources = environment.getPropertySources();

            Map<String, Object> map = new HashMap<>();
            map.put(CommonConstants.DUBBO_PREFERRED_NETWORK_INTERFACE, USE_NETWORK_INTERFACE_NAME);
            map.put(DUBBO_NETWORK_IGNORED_INTERFACE, IGNORED_NETWORK_INTERFACE_NAME);
            propertySources.addLast(new MapPropertySource("networkInterfaceConfig", map));
        }
    }

}
