package org.apache.dubbo.metrics.filter;

import org.apache.dubbo.common.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcException;

public class MetricsCollectExecutor {

    private DefaultMetricsCollector collector;

    private Invocation              invocation;

    private String                  interfaceName;

    private String                  methodName;

    private String                  group;

    private String                  version;


    private static final String     METRIC_FILTER_START_TIME = "metric_filter_start_time";


    public MetricsCollectExecutor(DefaultMetricsCollector collector, Invocation invocation) {
        init(invocation);

        this.collector = collector;

        this.invocation = invocation;
    }

    public void beforeExecute() {
        collector.increaseTotalRequests(interfaceName, methodName, group, version);
        collector.increaseProcessingRequests(interfaceName, methodName, group, version);
        invocation.put(METRIC_FILTER_START_TIME, System.currentTimeMillis());
    }

    public void postExecute() {
        collector.increaseSucceedRequests(interfaceName, methodName, group, version);
        endExecute();
    }

    public void throwExecute(Throwable throwable){
        if (throwable instanceof RpcException) {
            collector.increaseFailedRequests(interfaceName, methodName, group, version);
        }
        endExecute();
    }

    private void endExecute(){
        Long endTime = System.currentTimeMillis();
        Long beginTime = Long.class.cast(invocation.get(METRIC_FILTER_START_TIME));
        Long rt = endTime - beginTime;

        collector.addRT(interfaceName, methodName, group, version, rt);
        collector.decreaseProcessingRequests(interfaceName, methodName, group, version);
    }

    private void init(Invocation invocation) {
        String serviceUniqueName = invocation.getTargetServiceUniqueName();
        String methodName = invocation.getMethodName();
        String group = null;
        String interfaceAndVersion;
        String[] arr = serviceUniqueName.split("/");
        if (arr.length == 2) {
            group = arr[0];
            interfaceAndVersion = arr[1];
        } else {
            interfaceAndVersion = arr[0];
        }

        String[] ivArr = interfaceAndVersion.split(":");
        String interfaceName = ivArr[0];
        String version = ivArr.length == 2 ? ivArr[1] : null;

        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.group = group;
        this.version = version;
    }
}
