package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.utils.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GroupServiceKeyCache {

    private final String serviceGroup;

    //ConcurrentMap<serviceName, ConcurrentMap<serviceVersion, ConcurrentMap<port, String>>>
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer, String>>> serviceKeyMap;

    public GroupServiceKeyCache(String serviceGroup) {
        this.serviceGroup = serviceGroup;
        this.serviceKeyMap = new ConcurrentHashMap<>(512);
    }

    public String getServiceKey(String serviceName, String serviceVersion, int port) {
        ConcurrentMap<String, ConcurrentMap<Integer, String>> versionMap = serviceKeyMap.get(serviceName);
        if (versionMap == null) {
            serviceKeyMap.putIfAbsent(serviceName, new ConcurrentHashMap<>());
            versionMap = serviceKeyMap.get(serviceName);
        }

        serviceVersion = serviceVersion == null ? "" : serviceVersion;
        ConcurrentMap<Integer, String> portMap = versionMap.get(serviceVersion);
        if (portMap == null) {
            versionMap.putIfAbsent(serviceVersion, new ConcurrentHashMap<>());
            portMap = versionMap.get(serviceVersion);
        }

        String serviceKey = portMap.get(port);
        if (serviceKey == null) {
            serviceKey = createServiceKey(serviceName, serviceVersion, port);
            portMap.put(port, serviceKey);
        }
        return serviceKey;
    }

    private String createServiceKey(String serviceName, String serviceVersion, int port) {
        StringBuilder buf = new StringBuilder();
        if (StringUtils.isNotEmpty(serviceGroup)) {
            buf.append(serviceGroup).append('/');
        }

        buf.append(serviceName);
        if (StringUtils.isNotEmpty(serviceVersion) && !"0.0.0".equals(serviceVersion)) {
            buf.append(':').append(serviceVersion);
        }
        buf.append(':').append(port);
        return buf.toString();
    }
}
