/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.util;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.MonitorConfig;
import org.apache.dubbo.monitor.Constants;
import org.apache.dubbo.monitor.MonitorFactory;
import org.apache.dubbo.monitor.MonitorService;


import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_MONITOR_ADDRESS;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOCALHOST_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.SERVICE_REGISTRY_PROTOCOL;
import static org.apache.dubbo.config.Constants.DUBBO_IP_TO_REGISTRY;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

public class MonitorUrlUtil {

    public static URL loadMonitor(AbstractInterfaceConfig interfaceConfig, URL registryURL) {
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
                if (interfaceConfig.getScopeModel().getExtensionLoader(MonitorFactory.class).hasExtension(Constants.LOGSTAT_PROTOCOL)) {
                    map.put(PROTOCOL_KEY, Constants.LOGSTAT_PROTOCOL);
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
