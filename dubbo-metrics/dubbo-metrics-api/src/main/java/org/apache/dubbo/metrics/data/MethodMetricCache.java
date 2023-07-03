package org.apache.dubbo.metrics.data;

import org.apache.dubbo.metrics.model.MethodMetric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * metrics data cache
 */
public class MethodMetricCache {

    private static final Map<String, MethodMetric> METHOD_METRIC_CACHE = new ConcurrentHashMap<>();


    public static void put(String key, MethodMetric methodMetric) {
        METHOD_METRIC_CACHE.put(key, methodMetric);
    }
    public static void putIfAbsent(String key, MethodMetric methodMetric) {
        METHOD_METRIC_CACHE.putIfAbsent(key, methodMetric);
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
