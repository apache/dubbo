package org.apache.dubbo.monitor.support;


import com.alibaba.metrics.common.MetricObject;
import org.apache.dubbo.monitor.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AggregationMetrics {
    // ip -> metric -> MetricObject
    public volatile static Map<String, ConcurrentHashMap<String, MetricObject>> metrics = new ConcurrentHashMap<>();

    public static final String SUCCESS_RATE = "success_rate";
    public static final String QPS = "qps";

    public static Object getMetricsInfoByIp(String ip, String key) {
        String metric = Constants.DUBBO_IP + "." + key;
        Map<String, MetricObject> metricObjectMap = metrics.get(ip);
        if (metricObjectMap == null) {
            System.out.println("no ip " + ip + "metrics info");
            return null;
        }
        if (metricObjectMap.get(metric) != null) {
            return metricObjectMap.get(metric).getValue();
        }
        System.out.println("no ip [" + ip + "] metric [" + metric
                + "] info");
        return null;
    }

    public static void putMetrics(String ip, String metric, MetricObject value) {
        ConcurrentHashMap metricObjectMap = new ConcurrentHashMap<>();
        ConcurrentHashMap old = metrics.putIfAbsent(ip, metricObjectMap);
        if (old != null) {
            metricObjectMap = old;
        }
        metricObjectMap.put(metric, value);
    }

    public static void resetSuccessRate(String ip) {
        String name = Constants.DUBBO_IP + "." + AggregationMetrics.SUCCESS_RATE;
        AggregationMetrics.putMetrics(ip, name, new MetricObject.Builder(name).withValue(1).build());
    }

}
