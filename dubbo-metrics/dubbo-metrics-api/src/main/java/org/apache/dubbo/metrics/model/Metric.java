package org.apache.dubbo.metrics.model;

import java.util.Map;

public interface Metric {
    Map<String, String> getTags();
}
