package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.rpc.cluster.loadbalance.statistics.CpuUsageService;
import org.apache.dubbo.rpc.cluster.loadbalance.statistics.CpuUsageServiceImpl;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsLoadBalance extends AbstractLoadBalance {

    private Map<String, Float> cpuUsage = new HashMap<>();

    public StatisticsLoadBalance() {
        long timeToLive = Long.parseLong(ConfigurationUtils.getProperty("time.to.live"));
        long collectCpuUsageInMill = Long.parseLong(ConfigurationUtils.getProperty("mill.to.collect.cpu.usage"));
        CpuUsageService cpuUsageService = new CpuUsageServiceImpl(timeToLive, collectCpuUsageInMill);
        cpuUsageService.addListener("statisticsloadbalance", (ip, cpu) -> cpuUsage.put(ip, cpu));
    }

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        throw new IllegalStateException("Method unimplemented");
    }
}
