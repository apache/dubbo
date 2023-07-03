package org.apache.dubbo.metrics.model;

import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * metrics data cache
 */
public class MethodMetricCache {

    private static final Map<String, MethodMetric> METHOD_METRIC_CACHE = new ConcurrentHashMap<>();


    public static MethodMetric putIfAbsent(String key, MethodMetric methodMetric) {
        return METHOD_METRIC_CACHE.putIfAbsent(key, methodMetric);
    }

    public static MethodMetric putIfAbsent(String key, ApplicationModel applicationModel, Invocation invocation) {
        return METHOD_METRIC_CACHE.putIfAbsent(key, new MethodMetric(applicationModel, invocation));
    }

    public static void remove(String key) {
        METHOD_METRIC_CACHE.remove(key);
    }

    public static boolean containsKey(String key) {
        return METHOD_METRIC_CACHE.containsKey(key);
    }


    public static MethodMetric get(String key) {
        return METHOD_METRIC_CACHE.get(key);
    }


}
