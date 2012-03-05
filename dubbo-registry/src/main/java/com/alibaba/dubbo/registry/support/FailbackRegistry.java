/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.registry.NotifyListener;

/**
 * FailbackRegistry
 * 
 * @author william.liangf
 */
public abstract class FailbackRegistry extends AbstractRegistry {

    // 重试周期
    private static final int DEFAULT_RETRY_PERIOD =  5 * 1000;
    
    // 定时任务执行器
    private final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryFailedRetryTimer", true));

    // 失败重试定时器，定时检查是否有请求失败，如有，无限次重试
    private final ScheduledFuture<?> retryFuture;
    
    private final Set<String> failedRegistered = new ConcurrentHashSet<String>();

    private final Set<String> failedUnregistered = new ConcurrentHashSet<String>();
    
    private final ConcurrentMap<String, Set<NotifyListener>> failedSubscribed = new ConcurrentHashMap<String, Set<NotifyListener>>();
    
    private final ConcurrentMap<String, Set<NotifyListener>> failedUnsubscribed = new ConcurrentHashMap<String, Set<NotifyListener>>();

    private final ConcurrentMap<String, Map<NotifyListener, List<URL>>> failedNotified = new ConcurrentHashMap<String, Map<NotifyListener, List<URL>>>();
    
    public FailbackRegistry(URL url) {
        super(url);
        int retryPeriod = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, DEFAULT_RETRY_PERIOD);
        this.retryFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 检测并连接注册中心
                try {
                    retry();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
                }
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }
    
    // 重试失败的动作
    private void retry() throws Exception {
        if (! failedRegistered.isEmpty()) {
            Set<String> failed = new HashSet<String>(failedRegistered);
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry register " + failed);
                }
                try {
                    for (String url : failed) {
                        try {
                            doRegister(URL.valueOf(url));
                            failedRegistered.remove(url);
                        } catch (Throwable t) { // 忽略所有异常，等待下次重试
                            logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if(! failedUnregistered.isEmpty()) {
            Set<String> failed = new HashSet<String>(failedUnregistered);
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry unregister " + failed);
                }
                try {
                    for (String url : failed) {
                        try {
                            doUnregister(URL.valueOf(url));
                            failedUnregistered.remove(url);
                        } catch (Throwable t) { // 忽略所有异常，等待下次重试
                            logger.warn("Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (! failedSubscribed.isEmpty()) {
            Map<String, Set<NotifyListener>> failed = new HashMap<String, Set<NotifyListener>>(failedSubscribed);
            for (Map.Entry<String, Set<NotifyListener>> entry : failed.entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry subscribe " + failed);
                }
                try {
                    for (Map.Entry<String, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = URL.valueOf(entry.getKey());
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                doSubscribe(url, listener);
                                listeners.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (! failedUnsubscribed.isEmpty()) {
            Map<String, Set<NotifyListener>> failed = new HashMap<String, Set<NotifyListener>>(failedUnsubscribed);
            for (Map.Entry<String, Set<NotifyListener>> entry : failed.entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry unsubscribe " + failed);
                }
                try {
                    for (Map.Entry<String, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = URL.valueOf(entry.getKey());
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                doUnsubscribe(url, listener);
                                listeners.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        if (! failedNotified.isEmpty()) {
            Map<String, Map<NotifyListener, List<URL>>> failed = new HashMap<String, Map<NotifyListener, List<URL>>>(failedNotified);
            for (Map.Entry<String, Map<NotifyListener, List<URL>>> entry : failed.entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry notify " + failed);
                }
                try {
                    for (Map<NotifyListener, List<URL>> values : failed.values()) {
                        for (Map.Entry<NotifyListener, List<URL>> entry : values.entrySet()) {
                            try {
                                NotifyListener listener = entry.getKey();
                                List<URL> urls = entry.getValue();
                                listener.notify(urls);
                                values.remove(listener);
                            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                                logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        doRetry();
    }
    
    public void destroy() {
        super.destroy();
        try {
            retryFuture.cancel(true);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }
    
    public void register(URL url, NotifyListener listener) {
        super.register(url, listener);
        try {
            // 向服务器端发送注册请求
            doRegister(url);
            removeFailedRegistered(url);
        } catch (Exception t) {
            if (getUrl().getParameter(Constants.CHECK_KEY, true)) { // 如果开启了启动时检测，则直接抛出异常
                throw new IllegalStateException("Failed to register " + url + ", cause: " + t.getMessage(), t);
            }
            // 否则，将失败的注册请求记录到失败列表，定时重试
            failedRegistered.add(url.toFullString());
            logger.error("Failed to register " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }

    public void unregister(URL url, NotifyListener listener) {
        super.unregister(url, listener);
        try {
            // 向服务器端发送取消注册请求
            doUnregister(url);
            removeFailedRegistered(url);
        } catch (Exception t) {
            if (getUrl().getParameter(Constants.CHECK_KEY, true)) { // 如果开启了启动时检测，则直接抛出异常
                throw new IllegalStateException("Failed to uregister " + url + ", cause: " + t.getMessage(), t);
            }
            // 否则，将失败的取消注册请求记录到失败列表，定时重试
            failedUnregistered.add(url.toFullString());
            logger.error("Failed to uregister " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }

    private void removeFailedRegistered(URL url) {
        String key = url.toFullString();
        failedRegistered.remove(key);
        failedUnregistered.remove(key);
    }

    private void removeFailedSubscribed(URL url, NotifyListener listener) {
        String key = url.toFullString();
        Set<NotifyListener> listeners = failedSubscribed.get(key);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscribed.get(key);
        if (listeners != null) {
            listeners.remove(listener);
        }
        Map<NotifyListener, List<URL>> notified = failedNotified.get(key);
        if (notified != null) {
            notified.remove(listener);
        }
    }

    public void subscribe(URL url, NotifyListener listener) {
        super.subscribe(url, listener);
        try {
            // 向服务器端发送订阅请求
            doSubscribe(url, listener);
            removeFailedSubscribed(url, listener);
        } catch (Exception t) {
            if (getUrl().getParameter(Constants.CHECK_KEY, true)) { // 如果开启了启动时检测，则直接抛出异常
                throw new IllegalStateException("Failed to subscribe " + url + ", cause: " + t.getMessage(), t);
            }
            // 否则，将失败的订阅请求记录到失败列表，定时重试
            String key = url.toFullString();
            Set<NotifyListener> listeners = failedSubscribed.get(key);
            if (listeners == null) {
                failedSubscribed.putIfAbsent(key, new ConcurrentHashSet<NotifyListener>());
                listeners = failedSubscribed.get(key);
            }
            listeners.add(listener);
            logger.error("Failed to subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }

    public void unsubscribe(URL url, NotifyListener listener) {
        super.unsubscribe(url, listener);
        try {
            // 向服务器端发送取消订阅请求
            doUnsubscribe(url, listener);
            removeFailedSubscribed(url, listener);
        } catch (Exception t) {
            if (getUrl().getParameter(Constants.CHECK_KEY, true)) { // 如果开启了启动时检测，则直接抛出异常
                throw new IllegalStateException("Failed to unsubscribe " + url + ", cause: " + t.getMessage(), t);
            }
            // 否则，将失败的取消订阅请求记录到失败列表，定时重试
            String key = url.toFullString();
            Set<NotifyListener> listeners = failedUnsubscribed.get(key);
            if (listeners == null) {
                failedUnsubscribed.putIfAbsent(key, new ConcurrentHashSet<NotifyListener>());
                listeners = failedUnsubscribed.get(key);
            }
            listeners.add(listener);
            logger.error("Failed to unsubscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }
    
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if (urls == null || urls.size() ==0) {
            return;
        }
        try {
            listener.notify(urls);
        } catch (Exception t) {
            // 将失败的通知请求记录到失败列表，定时重试
            String key = url.toFullString();
            Map<NotifyListener, List<URL>> values = failedNotified.get(key);
            if (values == null) {
                failedNotified.putIfAbsent(key, new ConcurrentHashMap<NotifyListener, List<URL>>());
                values = failedNotified.get(key);
            }
            values.put(listener, urls);
            logger.error("Failed to unsubscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }

    protected abstract void doRegister(URL url);
    
    protected abstract void doUnregister(URL url);
    
    protected abstract void doSubscribe(URL url, NotifyListener listener);
    
    protected abstract void doUnsubscribe(URL url, NotifyListener listener);

    protected void doRetry() {}

    public Set<String> getFailedRegistered() {
        return failedRegistered;
    }

    public Set<String> getFailedUnregistered() {
        return failedUnregistered;
    }

    public Map<String, Set<NotifyListener>> getFailedSubscribed() {
        return failedSubscribed;
    }

    public Map<String, Set<NotifyListener>> getFailedUnsubscribed() {
        return failedUnsubscribed;
    }

    public Map<String, Map<NotifyListener, List<URL>>> getFailedNotified() {
        return failedNotified;
    }

}