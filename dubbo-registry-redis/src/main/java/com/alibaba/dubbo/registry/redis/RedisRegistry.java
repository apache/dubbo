/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.registry.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;

/**
 * RedisRegistry
 * 
 * @author william.liangf
 */
public class RedisRegistry extends FailbackRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RedisRegistry.class);

    private static final int DEFAULT_REDIS_PORT = 6379;

    private static final String REGISTER = "register";

    private static final String UNREGISTER = "unregister";

    private static final String SUBSCRIBE = "subscribe";

    private static final String UNSUBSCRIBE = "unsubscribe";
    
    private final ConcurrentMap<String, Set<String>> notified = new ConcurrentHashMap<String, Set<String>>();

    private final Jedis jedis;
    
    private final NotifySub sub = new NotifySub();

    public RedisRegistry(URL url) {
        super(url);
        this.jedis = new Jedis(url.getHost(), url.getPort() == 0 ? DEFAULT_REDIS_PORT : url.getPort());
        this.jedis.connect();
    }

    public boolean isAvailable() {
        return jedis.isConnected();
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            jedis.disconnect();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
        try {
            jedis.quit();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    @Override
    public void doRegister(URL url) {
        jedis.publish(url.getServiceInterface(), REGISTER + " " + url.toFullString());
    }

    @Override
    public void doUnregister(URL url) {
        jedis.publish(url.getServiceInterface(), UNREGISTER + " " + url.toFullString());
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            jedis.psubscribe(sub, url.getServiceInterface());
        } else {
            jedis.subscribe(sub, url.getServiceInterface());
        }
        jedis.publish(url.getServiceInterface(), SUBSCRIBE + " " + url.toFullString());
    }
    
    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        jedis.publish(url.getServiceInterface(), UNSUBSCRIBE + " " + url.toFullString());
    }

    private class NotifySub extends JedisPubSub {

        @Override
        public void onMessage(String key, String msg) {
            if (msg.startsWith(REGISTER)) {
                URL url = URL.valueOf(msg.substring(REGISTER.length()).trim());
                registered(url);
            } else if (msg.startsWith(UNREGISTER)) {
                URL url = URL.valueOf(msg.substring(UNREGISTER.length()).trim());
                unregistered(url);
            } else if (msg.startsWith(SUBSCRIBE)) {
                URL url = URL.valueOf(msg.substring(SUBSCRIBE.length()).trim());
                List<URL> urls = lookup(url);
                if (urls != null && urls.size() > 0) {
                    for (URL u : urls) {
                        jedis.publish(url.getServiceInterface(), REGISTER + " " + u.toFullString());
                    }
                }
            } /*else if (msg.startsWith(UNSUBSCRIBE)) {
            }*/
        }

        @Override
        public void onPMessage(String pattern, String key, String msg) {
            onMessage(key, msg);
        }

        @Override
        public void onSubscribe(String key, int num) {
        }

        @Override
        public void onPSubscribe(String pattern, int num) {
        }

        @Override
        public void onUnsubscribe(String key, int num) {
        }

        @Override
        public void onPUnsubscribe(String pattern, int num) {
        }

    }

    protected void registered(URL url) {
        for (Map.Entry<String, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            String key = entry.getKey();
            URL subscribe = URL.valueOf(key);
            if (UrlUtils.isMatch(subscribe, url)) {
                Set<String> urls = notified.get(key);
                if (urls == null) {
                    notified.putIfAbsent(key, new ConcurrentHashSet<String>());
                    urls = notified.get(key);
                }
                urls.add(url.toFullString());
                List<URL> list = toList(urls);
                if (list != null && list.size() > 0) {
                    for (NotifyListener listener : entry.getValue()) {
                        notify(subscribe, listener, list);
                        synchronized (listener) {
                            listener.notify();
                        }
                    }
                }
            }
        }
    }

    protected void unregistered(URL url) {
        for (Map.Entry<String, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            String key = entry.getKey();
            URL subscribe = URL.valueOf(key);
            if (UrlUtils.isMatch(subscribe, url)) {
                Set<String> urls = notified.get(key);
                if (urls != null) {
                    urls.remove(url.toFullString());
                }
                List<URL> list = toList(urls);
                if (list != null && list.size() > 0) {
                    for (NotifyListener listener : entry.getValue()) {
                        notify(subscribe, listener, list);
                    }
                }
            }
        }
    }

    protected void subscribed(URL url, NotifyListener listener) {
        List<URL> urls = lookup(url);
        if (urls != null && urls.size() > 0) {
            notify(url, listener, urls);
        }
    }

    private List<URL> toList(Set<String> urls) {
        List<URL> list = new ArrayList<URL>();
        if (urls != null && urls.size() > 0) {
            for (String url : urls) {
                list.add(URL.valueOf(url));
            }
        }
        return list;
    }

    public void register(URL url, NotifyListener listener) {
        super.register(url, listener);
        registered(url);
    }

    public void unregister(URL url, NotifyListener listener) {
        super.unregister(url, listener);
        unregistered(url);
    }

    public void subscribe(URL url, NotifyListener listener) {
        super.subscribe(url, listener);
        subscribed(url, listener);
    }

    public void unsubscribe(URL url, NotifyListener listener) {
        super.unsubscribe(url, listener);
        notified.remove(url.toFullString());
    }

    public Map<String, Set<String>> getNotified() {
        return notified;
    }
}
