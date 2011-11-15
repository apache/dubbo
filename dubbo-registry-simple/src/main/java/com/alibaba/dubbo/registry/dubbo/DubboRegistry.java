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
import com.alibaba.dubbo.registry.RegistryService;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcConstants;

/**
 * DubboRegistry
 * 
 * @author william.liangf
 */
public class DubboRegistry extends FailbackRegistry {

    private final static Logger logger = LoggerFactory.getLogger(DubboRegistry.class); 

    // 重连检测周期3秒(单位毫秒)
    private static final int RECONNECT_PERIOD_DEFAULT = 3 * 1000;
    
    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryReconnectTimer", true));

    // 重连定时器，定时检查连接是否可用，不可用时，无限次重连
    private final ScheduledFuture<?> reconnectFuture;

    // 客户端获取过程锁，锁定客户端实例的创建过程，防止重复的客户端
    private final ReentrantLock clientLock = new ReentrantLock();

    private final Set<String> registered = new ConcurrentHashSet<String>();
    
    private final ConcurrentMap<String, NotifyListener> subscribed = new ConcurrentHashMap<String, NotifyListener>();
    
    private final Invoker<RegistryService> registryInvoker;
    
    private final RegistryService registryService;
    
    public DubboRegistry(Invoker<RegistryService> registryInvoker, RegistryService registryService) {
        super(registryInvoker.getUrl());
        this.registryInvoker = registryInvoker;
        this.registryService = registryService;
        // 启动重连定时器
        int reconnectPeriod = registryInvoker.getUrl().getParameter(RpcConstants.REGISTRY_RECONNECT_PERIOD_KEY, RECONNECT_PERIOD_DEFAULT);
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
             if (getUrl().getParameter(Constants.CHECK_KEY, true)) {
                 if (t instanceof RuntimeException) {
                     throw (RuntimeException) t;
                 }
                 throw new RuntimeException(t.getMessage(), t);
             }
             logger.error("Failed to connect to registry " + getUrl().getAddress() + " from provider/consumer " + NetUtils.getLocalHost() + " use dubbo " + Version.getVersion() + ", cause: " + t.getMessage(), t);
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
    
    public void destroy() {
        super.destroy();
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
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        if (logger.isInfoEnabled()){
            logger.info("Lookup: " + url);
        }
        return registryService.lookup(url);
    }
    
    public void register(URL url) {
        registered.add(url.toFullString());
        super.register(url);
    }

    protected void doRegister(URL url) {
        registryService.register(url);
    }

    public void unregister(URL url) {
        registered.remove(url.toFullString());
        super.unregister(url);
    }
    
    protected void doUnregister(URL url) {
        registryService.unregister(url);
    }

    public void subscribe(URL url, NotifyListener listener) {
        subscribed.put(url.toFullString(), listener);
        super.subscribe(url, listener);
    }

    protected void doSubscribe(URL url, NotifyListener listener) {
        registryService.subscribe(url, listener);
    }
    
    public void unsubscribe(URL url, NotifyListener listener) {
        subscribed.remove(url.toFullString());
        super.unsubscribe(url, listener);
    }

    protected void doUnsubscribe(URL url, NotifyListener listener) {
        registryService.unsubscribe(url, listener);
    }
    
}