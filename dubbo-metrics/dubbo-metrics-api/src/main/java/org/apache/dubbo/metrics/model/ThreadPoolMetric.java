package org.apache.dubbo.metrics.model;

import org.apache.dubbo.common.utils.ConfigUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

import static org.apache.dubbo.common.constants.MetricsConstants.*;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

public class ThreadPoolMetric {

    private String applicationName;

   // private String threadPoolName;

    private ThreadPoolExecutor threadPoolExecutor;


    public ThreadPoolMetric(String applicationName, ThreadPoolExecutor threadPoolExecutor) {
        this.applicationName = applicationName;
        this.threadPoolExecutor = threadPoolExecutor;
       // this.threadPoolName = threadPoolName;
    }

//    public String getThreadPoolName() {
//        return threadPoolName;
//    }
//
//    public void setThreadPoolName(String threadPoolName) {
//        this.threadPoolName = threadPoolName;
//    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadPoolMetric that = (ThreadPoolMetric) o;
        return applicationName.equals(that.applicationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationName);
    }

    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_PID, ConfigUtils.getPid()+"");
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, applicationName);

       // tags.put(TAG_THREAD_NAME, threadPoolName);
        return tags;
    }

    public double getCorePoolSize() {
        return threadPoolExecutor.getCorePoolSize();
    }

    public double getLargestPoolSize() {
        return threadPoolExecutor.getLargestPoolSize();
    }

    public double getMaximumPoolSize() {
        return threadPoolExecutor.getMaximumPoolSize();
    }

    public double getActiveCount() {
        return threadPoolExecutor.getActiveCount();
    }

    public double getPoolSize(){
        return threadPoolExecutor.getPoolSize();
    }

    public double getQueueSize(){
        return threadPoolExecutor.getQueue().size();
    }

}
