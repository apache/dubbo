package org.apache.dubbo.metrics.model;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.MetricsConstants.*;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

public class ApplicationMetric implements Metric {
    private String applicationName;
    private String version;

    public ApplicationMetric(String applicationName, String version) {
        this.applicationName = applicationName;
        this.version = version;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getData() {
        return version;
    }

    public void setData(String version) {
        this.version = version;
    }

    @Override
    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, applicationName);

        tags.put(TAG_APPLICATION_VERSION_KEY, version);
        return tags;
    }
}
