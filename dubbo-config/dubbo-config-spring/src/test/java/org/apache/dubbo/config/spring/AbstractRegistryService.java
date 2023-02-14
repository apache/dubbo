/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.config.spring;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.RegistryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_FAILED_NOTIFY_EVENT;

/**
 * AbstractRegistryService
 */
public abstract class AbstractRegistryService implements RegistryService {

    // logger
    protected final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(getClass());

    // registered services
    // Map<serviceName, Map<url, queryString>>
    private final ConcurrentMap<String, List<URL>> registered = new ConcurrentHashMap<String, List<URL>>();

    // subscribed services
    // Map<serviceName, queryString>
    private final ConcurrentMap<String, Map<String, String>> subscribed = new ConcurrentHashMap<String, Map<String, String>>();

    // notified services
    // Map<serviceName, Map<url, queryString>>
    private final ConcurrentMap<String, List<URL>> notified = new ConcurrentHashMap<String, List<URL>>();

    // notification listeners for the subscribed services
    // Map<serviceName, List<notificationListener>>
    private final ConcurrentMap<String, List<NotifyListener>> notifyListeners = new ConcurrentHashMap<String, List<NotifyListener>>();

    @Override
    public void register(URL url) {
        if (logger.isInfoEnabled()) {
            logger.info("Register service: " + url.getServiceKey() + ",url:" + url);
        }
        register(url.getServiceKey(), url);
    }

    @Override
    public void unregister(URL url) {
        if (logger.isInfoEnabled()) {
            logger.info("Unregister service: " + url.getServiceKey() + ",url:" + url);
        }
        unregister(url.getServiceKey(), url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe service: " + url.getServiceKey() + ",url:" + url);
        }
        subscribe(url.getServiceKey(), url, listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (logger.isInfoEnabled()) {
            logger.info("Unsubscribe service: " + url.getServiceKey() + ",url:" + url);
        }
        unsubscribe(url.getServiceKey(), url, listener);
    }

    @Override
    public List<URL> lookup(URL url) {
        return getRegistered(url.getServiceKey());
    }

    public void register(String service, URL url) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        List<URL> urls = ConcurrentHashMapUtils.computeIfAbsent(registered, service, k -> new CopyOnWriteArrayList<>());
        if (!urls.contains(url)) {
            urls.add(url);
        }
    }

    public void unregister(String service, URL url) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        List<URL> urls = registered.get(service);
        if (urls != null) {
            URL deleteURL = null;
            for (URL u : urls) {
                if (u.toIdentityString().equals(url.toIdentityString())) {
                    deleteURL = u;
                    break;
                }
            }
            if (deleteURL != null) {
                urls.remove(deleteURL);
            }
        }
    }

    public void subscribe(String service, URL url, NotifyListener listener) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("parameters == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        subscribed.put(service, url.getParameters());
        addListener(service, listener);
    }

    public void unsubscribe(String service, URL url, NotifyListener listener) {
        if (service == null) {
            throw new IllegalArgumentException("service == null");
        }
        if (url == null) {
            throw new IllegalArgumentException("parameters == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }
        subscribed.remove(service);
        removeListener(service, listener);
    }

    private void addListener(final String service, final NotifyListener listener) {
        if (listener == null) {
            return;
        }
        List<NotifyListener> listeners = ConcurrentHashMapUtils.computeIfAbsent(notifyListeners, service, k -> new CopyOnWriteArrayList<>());
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void removeListener(final String service, final NotifyListener listener) {
        if (listener == null) {
            return;
        }
        List<NotifyListener> listeners = notifyListeners.get(service);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    private void doNotify(String service, List<URL> urls) {
        notified.put(service, urls);
        List<NotifyListener> listeners = notifyListeners.get(service);
        if (listeners != null) {
            for (NotifyListener listener : listeners) {
                try {
                    notify(service, urls, listener);
                } catch (Throwable t) {
                    logger.error(CONFIG_FAILED_NOTIFY_EVENT, "", "", "Failed to notify registry event, service: " + service + ", urls: " + urls + ", cause: " + t.getMessage(), t);
                }
            }
        }
    }

    protected void notify(String service, List<URL> urls, NotifyListener listener) {
        listener.notify(urls);
    }

    protected final void forbid(String service) {
        doNotify(service, new ArrayList<URL>(0));
    }

    protected final void notify(String service, List<URL> urls) {
        if (StringUtils.isEmpty(service)
            || CollectionUtils.isEmpty(urls)) {
            return;
        }
        doNotify(service, urls);
    }

    public Map<String, List<URL>> getRegistered() {
        return Collections.unmodifiableMap(registered);
    }

    public List<URL> getRegistered(String service) {
        return Collections.unmodifiableList(registered.get(service));
    }

    public Map<String, Map<String, String>> getSubscribed() {
        return Collections.unmodifiableMap(subscribed);
    }

    public Map<String, String> getSubscribed(String service) {
        return subscribed.get(service);
    }

    public Map<String, List<URL>> getNotified() {
        return Collections.unmodifiableMap(notified);
    }

    public List<URL> getNotified(String service) {
        return Collections.unmodifiableList(notified.get(service));
    }

    public Map<String, List<NotifyListener>> getListeners() {
        return Collections.unmodifiableMap(notifyListeners);
    }

}
