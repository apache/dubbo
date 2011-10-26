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
package com.alibaba.dubbo.registry.dubbo;

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
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.NamedThreadFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcConstants;

/**
 * DubboRegistry
 * 
 * @author william.liangf
 */
public class DubboRegistry implements Registry {

    private final static Logger logger = LoggerFactory.getLogger(DubboRegistry.class); 

    private static final int RETRY_FAILED_PERIOD_DEFAULT =  5 * 1000;
    
    // 重连检测周期3秒(单位毫秒)
    private static final int RECONNECT_PERIOD_DEFAULT = 3 * 1000;
    
    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, new NamedThreadFactory("DubboRegistryClientTimer", true));

    // 失败重试定时器，定时检查是否有请求失败，如有，无限次重试
    private final ScheduledFuture<?> retryFailedFuture;

    // 重连定时器，定时检查连接是否可用，不可用时，无限次重连
    private final ScheduledFuture<?> reconnectFuture;

    // 客户端获取过程锁，锁定客户端实例的创建过程，防止重复的客户端
    private final ReentrantLock clientLock = new ReentrantLock();

    private final Set<String> registered = new ConcurrentHashSet<String>();
    
    private final ConcurrentMap<String, NotifyListener> subscribed = new ConcurrentHashMap<String, NotifyListener>();
    
    // 失败的注册信息，定时重试
    private final Set<String> failedRegistered = new ConcurrentHashSet<String>();
    
    private final Set<String> failedUnregistered = new ConcurrentHashSet<String>();

    // 失败的订阅信息，定时重试
    private final ConcurrentMap<String, NotifyListener> failedSubscribed = new ConcurrentHashMap<String, NotifyListener>();
    
    private final ConcurrentMap<String, NotifyListener> failedUnsubscribed = new ConcurrentHashMap<String, NotifyListener>();
    
    private final Invoker<RegistryService> registryInvoker;
    
    private final RegistryService registryService;
    
    public DubboRegistry(Invoker<RegistryService> registryInvoker, RegistryService registryService) {
        this.registryInvoker = registryInvoker;
        this.registryService = registryService;
        // 启动重连定时器
        int reconnectPeriod = registryInvoker.getUrl().getIntParameter(RpcConstants.REGISTRY_RECONNECT_PERIOD_KEY, RECONNECT_PERIOD_DEFAULT);
        reconnectFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 检测并连接注册中心
                try {
                    connect();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at reconnect, cause: " + t.getMessage(), t);
                }
            }
        }, reconnectPeriod, reconnectPeriod, TimeUnit.MILLISECONDS);
        int retryFailedPeriod = registryInvoker.getUrl().getIntParameter(RpcConstants.REGISTRY_RETRY_FAILED_PERIOD_KEY,RETRY_FAILED_PERIOD_DEFAULT);
        this.retryFailedFuture = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // 检测并连接注册中心
                try {
                    retryFailed();
                } catch (Throwable t) { // 防御性容错
                    logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
                }
            }
        }, retryFailedPeriod, retryFailedPeriod, TimeUnit.MILLISECONDS);
    }

    protected final void connect() {
        try {
            // 检查是否已连接
            if (isAvailable()) {
                return;
            }
            if (logger.isInfoEnabled()) {
                logger.info("Reconnect to registry " + getUrl());
            }
            clientLock.lock();
            try {
                // 双重检查是否已连接
                if (isAvailable()) {
                    return;
                }
                recover();
            } finally {
                clientLock.unlock();
            }
        } catch (Throwable t) { // 忽略所有异常，等待下次重试
             if (getUrl().getBooleanParameter(Constants.CHECK_KEY, true)) {
                 if (t instanceof RuntimeException) {
                     throw (RuntimeException) t;
                 }
                 throw new RuntimeException(t.getMessage(), t);
             }
             logger.error("Failed to connect to registry " + getUrl().getAddress() + " from provider/consumer " + NetUtils.getLocalHost() + " use dubbo " + Version.getVersion() + ", cause: " + t.getMessage(), t);
        }
    }

    // 重试失败的动作
    private void retryFailed() throws Exception {
        if (! failedRegistered.isEmpty()) {
            Set<String> failed = new HashSet<String>(failedRegistered);
            if (logger.isInfoEnabled()) {
                logger.info("Retry register services " + failed);
            }
            if (! failed.isEmpty()) {
                try {
                    for (String url : failed) {
                        registryService.register(URL.valueOf(url));
                        failedRegistered.remove(url);
                    }
                } catch (Throwable t) { // 忽略所有异常，等待下次重试
                    logger.warn("Failed to retry register services " + failed, t);
                }
            }
        }
        if(! failedUnregistered.isEmpty()) {
            Set<String> failed = new HashSet<String>(failedUnregistered);
            if (logger.isInfoEnabled()) {
                logger.info("Retry unregister services " + failed);
            }
            try {
                for (String url : failed) {
                    registryService.unregister(URL.valueOf(url));
                    failedUnregistered.remove(url);
                }
            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                logger.warn("Failed to retry unregister services " + failed, t);
            }
        }
        if (! failedSubscribed.isEmpty()) {
            Map<String, NotifyListener> failed = new HashMap<String, NotifyListener>(failedSubscribed);
            if (logger.isInfoEnabled()) {
                logger.info("Retry subscribe services " + failed);
            }
            try {
                for (Map.Entry<String, NotifyListener> entry : failed.entrySet()) {
                    String url = entry.getKey();
                    registryService.subscribe(URL.valueOf(url), entry.getValue());
                    failedSubscribed.remove(url);
                }
            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                logger.warn("Failed to retry subscribe services " + failed, t);
            }
        }
        if (! failedUnsubscribed.isEmpty()) {
            Map<String, NotifyListener> failed = new HashMap<String, NotifyListener>(failedUnsubscribed);
            if (logger.isInfoEnabled()) {
                logger.info("Retry unsubscribe services " + failed);
            }
            try {
                for (Map.Entry<String, NotifyListener> entry : failed.entrySet()) {
                    String url = entry.getKey();
                    registryService.unsubscribe(URL.valueOf(url), entry.getValue());
                    failedSubscribed.remove(url);
                }
            } catch (Throwable t) { // 忽略所有异常，等待下次重试
                logger.warn("Failed to retry unsubscribe services " + failed, t);
            }
        }
    }

    protected final void recover() throws Exception {
        // register
        Set<String> recoverRegistered = new HashSet<String>(registered);
        if (! recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register services " + recoverRegistered);
            }
            for (String url : recoverRegistered) {
                register(URL.valueOf(url));
            }
        }
        // subscribe
        Map<String, NotifyListener> recoverSubscribed = new HashMap<String, NotifyListener>(subscribed);
        if (recoverSubscribed.size() > 0) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe services " + recoverSubscribed);
            }
            for (Map.Entry<String, NotifyListener> entry : recoverSubscribed.entrySet()) {
                String url = entry.getKey();
                subscribe(URL.valueOf(url), entry.getValue());
            }
        }
    }

    public boolean isAvailable() {
        if (registryInvoker == null)
            return false;
        return registryInvoker.isAvailable();
    }

    public URL getUrl() {
        return registryInvoker.getUrl();
    }

    public void destroy() {
        if (logger.isInfoEnabled()){
            logger.info("Destroy registry: " + getUrl());
        }
        try {
            // 取消失败重试定时器
            if (! retryFailedFuture.isCancelled()) {
                retryFailedFuture.cancel(true);
            }
        } catch (Throwable t) {
            logger.warn("Failed to cancel retry timer", t);
        }
        try {
            // 取消重连定时器
            if (! reconnectFuture.isCancelled()) {
                reconnectFuture.cancel(true);
            }
        } catch (Throwable t) {
            logger.warn("Failed to cancel reconnect timer", t);
        }
        registryInvoker.destroy();
    }

    public List<URL> lookup(URL url) {
        if (logger.isInfoEnabled()){
            logger.info("Lookup: " + url);
        }
        return registryService.lookup(url);
    }

    public void register(URL url) {
        if (logger.isInfoEnabled()){
            logger.info("Register: " + url);
        }
        try {
            registryService.register(url);
            registered.add(url.toFullString());
        } catch (Exception t) {
            if (getUrl().getBooleanParameter(Constants.CHECK_KEY, true)) {
                throw new IllegalStateException("Failed to register " + url + ", cause: " + t.getMessage(), t);
            }
            // 记录失败，定时重试
            failedRegistered.add(url.toFullString());
            logger.error("Failed to register " + url + ", cause: " + t.getMessage(), t);
        }
    }

    public void unregister(URL url) {
        if (logger.isInfoEnabled()){
            logger.info("Unregister: " + url);
        }
        try {
            registryService.unregister(url);
            registered.remove(url.toFullString());
        } catch (Exception t) {
            if (getUrl().getBooleanParameter(Constants.CHECK_KEY, true)) {
                throw new IllegalStateException("Failed to unregister " + url + ", cause: " + t.getMessage(), t);
            }
            // 记录失败，定时重试
            failedUnregistered.add(url.toFullString());
            logger.error("Failed to unregister " + url + ", cause: " + t.getMessage(), t);
        }
    }

    public void subscribe(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()){
            logger.info("Subscribe: " + url);
        }
        try {
            registryService.subscribe(url, listener);
            subscribed.put(url.toFullString(), listener);
        } catch (Exception t) {
            if (getUrl().getBooleanParameter(Constants.CHECK_KEY, true)) {
                throw new IllegalStateException("Failed to subscribe " + url + ", cause: " + t.getMessage(), t);
            }
            // 记录失败，定时重试
            failedSubscribed.put(url.toFullString(), listener);
            logger.error("Failed to subscribe " + url + ", cause: " + t.getMessage(), t);
        }
    }
    
    public void unsubscribe(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()){
            logger.info("Unsubscribe: " + url);
        }
        try {
            registryService.unsubscribe(url, listener);
            subscribed.remove(url.toFullString());
        } catch (Exception t) {
            if (getUrl().getBooleanParameter(Constants.CHECK_KEY, true)) {
                throw new IllegalStateException("Failed to unsubscribe " + url + ", cause: " + t.getMessage(), t);
            }
            // 记录失败，定时重试
            failedUnsubscribed.put(url.toFullString(), listener);
            logger.error("Failed to unsubscribe " + url + ", cause: " + t.getMessage(), t);
        }
    }

}