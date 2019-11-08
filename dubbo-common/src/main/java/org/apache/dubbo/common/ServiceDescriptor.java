package org.apache.dubbo.common;

import org.apache.dubbo.common.utils.StringUtils;

/**
 * 2019-10-10
 */
public class ServiceDescriptor {
    public static final char COLON_SEPERATOR = ':';

    protected String serviceKey;
    protected String serviceInterfaceName;
    protected String version;
    protected volatile String group;

    public static String buildServiceKey(String path, String group, String version) {
        StringBuilder buf = new StringBuilder();
        if (group != null && group.length() > 0) {
            buf.append(group).append("/");
        }
        buf.append(path);
        if (version != null && version.length() > 0) {
            buf.append(":").append(version);
        }
        return buf.toString();
    }

    /**
     * Format : interface:version:group
     *
     * @return
     */
    public String getDisplayServiceKey() {
        StringBuilder serviceNameBuilder = new StringBuilder();
        serviceNameBuilder.append(serviceInterfaceName);
        serviceNameBuilder.append(COLON_SEPERATOR).append(version);
        serviceNameBuilder.append(COLON_SEPERATOR).append(group);
        return serviceNameBuilder.toString();
    }

    public static ServiceDescriptor revertDisplayServiceKey(String displayKey) {
        String[] eles = StringUtils.split(displayKey, COLON_SEPERATOR);
        if (eles == null || eles.length < 1 || eles.length > 3) {
            return new ServiceDescriptor();
        }
        ServiceDescriptor serviceDescriptor = new ServiceDescriptor();
        serviceDescriptor.setServiceInterfaceName(eles[0]);
        if (eles.length > 1) {
            serviceDescriptor.setVersion(eles[1]);
        }
        if (eles.length == 3) {
            serviceDescriptor.setGroup(eles[2]);
        }

        return serviceDescriptor;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void generateServiceKey() {
        this.serviceKey = buildServiceKey(serviceInterfaceName, group, version);
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getServiceInterfaceName() {
        return serviceInterfaceName;
    }

    public void setServiceInterfaceName(String serviceInterfaceName) {
        this.serviceInterfaceName = serviceInterfaceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

}
