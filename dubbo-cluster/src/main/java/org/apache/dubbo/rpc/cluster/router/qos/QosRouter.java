package org.apache.dubbo.rpc.cluster.router.qos;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.monitor.support.AggregationMetrics;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class QosRouter extends AbstractRouter {

    private ConcurrentHashMap<URL, Object> candidateURLs = new ConcurrentHashMap<URL, Object>();
    private ConcurrentHashMap<URL, Object> isolatedURLs = new ConcurrentHashMap<URL, Object>();
    private static final Object PRESENT_FLAG = new Object();

    private Semaphore refreshPermit = new Semaphore(1);
    private Semaphore recoverPermit = new Semaphore(1);

    private ConcurrentHashMap<URL, IsolationContext> isolationContexts = new ConcurrentHashMap<URL, IsolationContext>();

    private static final int QOS_STATE_OK = 0;
    private static final int QOS_STATE_ERROR = 1;
    private static final int QOS_STATE_UNDETERMINED = 2;

    private volatile QosConfig qosConfig = new QosConfig();

    protected volatile List<URL> targetURLs = new ArrayList<URL>();

    private final ScheduledExecutorService REFRESH_EXECUTOR = Executors.newScheduledThreadPool(1, new NamedThreadFactory("QosRouterCheckQos", true));
    private final ScheduledExecutorService RECOVER_EXECUTOR = Executors.newScheduledThreadPool(1, new NamedThreadFactory("QosRouterCheckQos", true));

    public QosRouter(URL url) {
        this.url = url;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        System.out.println("QosRouter success_rate[" + AggregationMetrics.getMetricsInfoByIp(url.getIp(), AggregationMetrics.SUCCESS_RATE) + "]");
        System.out.println("QosRouter qps[" +
                AggregationMetrics.getMetricsInfoByIp(url.getIp(), AggregationMetrics.QPS) + "]");
        List<Invoker<T>> targetInvokers = new ArrayList<>();
        if (isCircuitBreakerEnabled(qosConfig)) {

            int qosState = checkQos(url);
            boolean hasNewCandidate = false;
            if (QOS_STATE_OK == qosState) {
                IsolationContext isolationContext = tryGetIsolationContext(url);
                if (isolationContext != null) {
                    int oldIsolationTimeMultiple = isolationContext.resetIsolationTimeMultiple();
                    if (oldIsolationTimeMultiple > 1) {
                        // log
                    }
                }
            } else if (QOS_STATE_ERROR == qosState) {
                if (candidateURLs.putIfAbsent(url, PRESENT_FLAG) == null) {
                    hasNewCandidate = true;
                    IsolationContext isolationContext = getIsolationContext(url);
                    isolationContext.setRecoverTime(System.currentTimeMillis() + calculateIsolationTime(isolationContext));
                    System.out.println(url.getIp() + "put into candidate list");
                }
                int maxIsolateCount = maxIsolateCount();
                int isolatedCount = isolatedURLs.size();
                if (isolatedCount >= maxIsolateCount) {
                    // log
                }
            }

            if (hasNewCandidate) {
                //check if it needed to join isolated urls
                refresh();
                //remove timeout service url from isolated and candidate urls
                recover();
            }

            // remove isolatedURLS from all address
            targetInvokers.clear();
            List<URL> availableAddresses = getAvailableAddresses();
            for (Invoker invoker : invokers) {
                URL invokerUrl = invoker.getUrl();
                if (availableAddresses.contains(invokerUrl)) {
                    targetInvokers.add(invoker);
                }
            }
            System.out.println(targetInvokers.size());
            return targetInvokers;
        }

        return invokers;
    }

    private long calculateIsolationTime(IsolationContext isolationContext) {
        long isolationTime;
        if (isolationContext.getIsolationTimeMultiple() <= qosConfig.getMaxIsolationTimeMultiple()) {
            isolationTime = isolationContext.isolationTimeMultiple * qosConfig.getIsolationTime();
        } else {
            isolationTime = qosConfig.getMaxIsolationTimeMultiple() * qosConfig.getIsolationTime();
        }

        if (isolationTime <= 0) {
            isolationTime = qosConfig.getIsolationTime();
        }
        return isolationTime;
    }

    private List<URL> getAvailableAddresses() {
        List<URL> result = new ArrayList<URL>(targetURLs.size());
        result.addAll(targetURLs);
        //todo isolatedURLs 改成 isolatedIps
        if (isCircuitBreakerEnabled(qosConfig)) {
            for (URL isolatedAddress : isolatedURLs.keySet()) {
                for (URL url : targetURLs) {
                    if (url.getIp().equals(isolatedAddress.getIp())) {
                        result.remove(url);
                    }
                }
            }
        }
        return result;
    }

    private int maxIsolateCount() {
        if (targetURLs == null) {
            return 0;
        }

        if (targetURLs.size() < 1) {
            return 0;
        } else {
            int isolationCount = (int) (targetURLs.size() * qosConfig.getMaxIsolationRate());
            return isolationCount == 0 ? 1 : isolationCount;
        }
    }

    private IsolationContext getIsolationContext(URL url) {
        IsolationContext isolationContext = isolationContexts.get(url);
        if (isolationContext == null) {
            isolationContext = new IsolationContext();
            IsolationContext old = isolationContexts.putIfAbsent(url, isolationContext);
            if (old != null) {
                isolationContext = old;
            }
        }
        return isolationContext;
    }

    public IsolationContext tryGetIsolationContext(URL url) {
        return isolationContexts.get(url);
    }

    private int checkQos(URL url) {
        try {
            String ip = url.getIp();
            double qps = Double.valueOf(AggregationMetrics.getMetricsInfoByIp(ip, AggregationMetrics.QPS) + "");
            double success_rate = Double.valueOf(AggregationMetrics.getMetricsInfoByIp(ip, AggregationMetrics.SUCCESS_RATE) + "");
            if (qps <= 0) {
                return QOS_STATE_UNDETERMINED;
            }
            if (success_rate < (1 - qosConfig.getErrorRateThreshold())) {
                System.out.println("QOS_STATE_ERROR");
                return QOS_STATE_ERROR;
            }
            return QOS_STATE_OK;
        } catch (Throwable throwable) {
            return QOS_STATE_UNDETERMINED;
        }
    }

    private boolean isCircuitBreakerEnabled(QosConfig qosConfig) {
        return qosConfig != null && qosConfig.isQosEnabled() && qosConfig.getIsolationTime() > 0;
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        this.targetURLs.clear();
        for (Invoker invoker : invokers) {
            this.targetURLs.add(invoker.getUrl());
        }

        refresh();
    }

    private void refresh() {
        if (refreshPermit.tryAcquire()) {
            REFRESH_EXECUTOR.submit(new Runnable() {
                @Override
                public void run() {
                    refreshPermit.release();

                    if (isCircuitBreakerEnabled(qosConfig)) {
                        final int maxIsolateCount = maxIsolateCount();
                        int isolateCellLeft = maxIsolateCount - isolatedURLs.size();

                        if (isolateCellLeft > 0) {
                            URL[] candidates = candidateURLs.keySet().toArray(new URL[0]);
                            for (URL candidate : candidates) {
                                // if not isolated yet
                                if (isolatedURLs.putIfAbsent(candidate, PRESENT_FLAG) == null) {
                                    IsolationContext isolationContext = getIsolationContext(candidate);
                                    System.out.println(candidate.getIp() + "put into isolation list");
                                    // increase isolation time num
                                    isolationContext.addIsolationTimeMultiple();
                                    if (--isolateCellLeft <= 0) {
                                        break;
                                    }
                                }
                            }
                        } else if (isolateCellLeft < 0) {
                            //remove from isolation list due to MAX_ISOLATION_RATE limit
                            int recoverCount = Math.abs(isolateCellLeft);
                            URL[] isolatedArray = isolatedURLs.keySet().toArray(new URL[0]);
                            for (int i = 0; i < recoverCount; i++) {
                                if (i < isolatedArray.length) {
                                    isolatedURLs.remove(isolatedArray[i]);
                                    System.out.println(isolatedArray[i].getIp() + "remove from isolation list");
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void recover() {
        if (recoverPermit.tryAcquire()) {
            RECOVER_EXECUTOR.schedule(new Runnable() {
                @Override
                public void run() {
                    recoverPermit.release();
                    try {
                        final long now = System.currentTimeMillis();
                        final URL[] isolatedArray = isolatedURLs.keySet().toArray(new URL[0]);
                        boolean needRefresh = false;
                        for (URL address : isolatedArray) {
                            //todo isolatedURLs 改成 isolatedIps
                            boolean contaions = false;
                            for (URL url : targetURLs) {
                                if (url.getIp().equals(address.getIp())) {
                                    contaions = true;
                                }
                            }
                            if (contaions) {
                                IsolationContext isolationContext = getIsolationContext(address);
                                if (isolationContext.getRecoverTime() < now) {
                                    needRefresh = true;
                                    candidateURLs.remove(address);
                                    isolatedURLs.remove(address);
                                    AggregationMetrics.resetSuccessRate(address.getIp());
                                    System.out.println(address.getIp() + "remove from isolation list due to timeout");
//                                    LOGGER.info("[QosRouter][" + serviceMetadata.getUniqueName() + "] remove from isolation list due to timeout: " + address);
                                }
                            } else {
                                needRefresh = true;
                                candidateURLs.remove(address);
                                isolatedURLs.remove(address);
                                isolationContexts.remove(address);
                                System.out.println(address.getIp() + "remove server from isolation list");
//                                LOGGER.info("[QosRouter][" + serviceMetadata.getUniqueName() + "] remove phantom server from isolation list : " + address);
                            }
                        }

                        //clear candidate
                        URL[] candidates = candidateURLs.keySet().toArray(new URL[0]);
                        for (URL candidate : candidates) {
                            IsolationContext isolationContext = getIsolationContext(candidate);
                            if (isolationContext.getRecoverTime() < now) {
                                candidateURLs.remove(candidate);
                                System.out.println(candidate.getIp() + "remove from candidate list due to timeout:");
//                                LOGGER.info("[QosRouter][" + serviceMetadata.getUniqueName() + "] remove from candidate list due to timeout: " + candidate);
                            }
                        }

                        if (needRefresh) {
                            refresh();
                        }

                        //have more to recover
                        if (!isolatedURLs.isEmpty() || !candidateURLs.isEmpty()) {
                            recover();
                        }
                    } catch (Throwable throwable) {
                        refresh();
                        recover();
                    }
                }
            }, 10, TimeUnit.SECONDS);
        }
    }

    static class IsolationContext {
        private volatile int isolationTimeMultiple = 1;
        private volatile long recoverTime;

        public int resetIsolationTimeMultiple() {
            int old = isolationTimeMultiple;
            isolationTimeMultiple = 1;
            return old;
        }

        public void addIsolationTimeMultiple() {
            isolationTimeMultiple++;
        }

        public int getIsolationTimeMultiple() {
            return isolationTimeMultiple;
        }


        public long getRecoverTime() {
            return recoverTime;
        }

        public void setRecoverTime(long recoverTime) {
            this.recoverTime = recoverTime;
        }
    }
}

