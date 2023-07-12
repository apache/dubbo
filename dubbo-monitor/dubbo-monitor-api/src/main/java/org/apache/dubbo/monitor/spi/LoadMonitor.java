package org.apache.dubbo.monitor.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.constants.PackageName;
import org.apache.dubbo.common.constants.SpiMethods;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.config.deploy.lifecycle.manager.SpiMethodManager;
import org.apache.dubbo.monitor.MonitorService;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.MonitorConstants.LOGSTAT_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.common.constants.ClusterConstants.REFER_KEY;

public class LoadMonitor implements SpiMethod {

    @Override
    public SpiMethods methodName() {
        return SpiMethods.loadMonitor;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    /**
     * Load SPI Monitor.
     *
     * @return monitor URL
     */
    @Override
    public Object invoke(Object... params) {

        AbstractInterfaceConfig interfaceConfig = (AbstractInterfaceConfig) params[0];
        URL registryURL = (URL) params[1];

        Map<String, String> map = new HashMap<String, String>();
        map.put(INTERFACE_KEY, MonitorService.class.getName());
        AbstractInterfaceConfig.appendRuntimeParameters(map);
        //set ip
        String hostToRegistry = ConfigUtils.getSystemProperty(DUBBO_IP_TO_REGISTRY);
        if (StringUtils.isEmpty(hostToRegistry)) {
            hostToRegistry = NetUtils.getLocalHost();
        } else if (NetUtils.isInvalidLocalHost(hostToRegistry)) {
            throw new IllegalArgumentException("Specified invalid registry ip from property:" +
                DUBBO_IP_TO_REGISTRY + ", value:" + hostToRegistry);
        }
        map.put(REGISTER_IP_KEY, hostToRegistry);

        MonitorConfig monitor = interfaceConfig.getMonitor();
        ApplicationConfig application = interfaceConfig.getApplication();
        AbstractConfig.appendParameters(map, monitor);
        AbstractConfig.appendParameters(map, application);
        String address = null;
        String sysAddress = System.getProperty(DUBBO_MONITOR_ADDRESS);
        if (sysAddress != null && sysAddress.length() > 0) {
            address = sysAddress;
        } else if (monitor != null) {
            address = monitor.getAddress();
        }
        String protocol = monitor == null ? null : monitor.getProtocol();
        if (monitor != null &&
            (REGISTRY_PROTOCOL.equals(protocol) || SERVICE_REGISTRY_PROTOCOL.equals(protocol))
            && registryURL != null) {
            return URLBuilder.from(registryURL)
                .setProtocol(DUBBO_PROTOCOL)
                .addParameter(PROTOCOL_KEY, protocol)
                .putAttribute(REFER_KEY, map)
                .build();
        } else if (ConfigUtils.isNotEmpty(address) || ConfigUtils.isNotEmpty(protocol)) {
            if (!map.containsKey(PROTOCOL_KEY)) {
                boolean supportMonitor;

                if(SpiMethodManager.get().packagePresent(PackageName.MONITOR)){
                    supportMonitor = (Boolean)SpiMethodManager.get().invoke(SpiMethods.isSupportMonitor,interfaceConfig);
                }else {
                    supportMonitor = false;
                }
                if (supportMonitor) {
                    map.put(PROTOCOL_KEY, LOGSTAT_PROTOCOL);
                } else if (ConfigUtils.isNotEmpty(protocol)) {
                    map.put(PROTOCOL_KEY, protocol);
                } else {
                    map.put(PROTOCOL_KEY, DUBBO_PROTOCOL);
                }
            }
            if (ConfigUtils.isEmpty(address)) {
                address = LOCALHOST_VALUE;
            }
            return UrlUtils.parseURL(address, map);
        }
        return null;
    }
}
