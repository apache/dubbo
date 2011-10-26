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
package com.alibaba.dubbo.registry.zookeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * ZookeeperRegistry
 * 
 * @author william.liangf
 */
public class ZookeeperRegistry implements Registry {

    private final static Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    private final URL           url;

    private final ZooKeeper     zookeeper;

    public ZookeeperRegistry(URL url) {
        this.url = url;
        try {
            this.zookeeper = new ZooKeeper(url.getAddress(), url.getPositiveIntParameter(
                    Constants.TIMEOUT_KEY, 5000), new Watcher() {
                public void process(WatchedEvent event) {
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public URL getUrl() {
        return url;
    }

    public boolean isAvailable() {
        return zookeeper.getState().isAlive();
    }

    public void destroy() {
        try {
            zookeeper.close();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    public void register(URL url) {
        try {
            String service = "/" + URL.encode(url.getServiceKey());
            if (zookeeper.exists(service, false) == null) {
                zookeeper.create(service, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            String provider = service + "/" + URL.encode(url.toIdentityString());
            if (zookeeper.exists(provider, false) == null) {
                zookeeper.create(provider, url.toParameterString().getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            }
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
    }

    public void unregister(URL url) {
        try {
            String service = "/" + URL.encode(url.getServiceKey());
            String provider = service + "/" + URL.encode(url.toIdentityString());
            zookeeper.delete(provider, -1);
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
    }

    public void subscribe(URL url, NotifyListener listener) {
        try {
            String service = "/" + URL.encode(url.getServiceKey());
            List<String> providers = zookeeper.getChildren(service, new NotifyWatcher(url, listener));
            listener.notify(toUrls(service, providers));
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
    }

    public void unsubscribe(URL url, NotifyListener listener) {
    }

    public List<URL> lookup(URL url) {
        try {
            String service = "/" + URL.encode(url.getServiceKey());
            List<String> providers = zookeeper.getChildren(service, false);
            return toUrls(service, providers);
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
    }
    
    private List<URL> toUrls(String service, List<String> providers) throws KeeperException, InterruptedException {
        List<URL> urls = new ArrayList<URL>();
        for (String provider : providers) {
            String path = service + provider;
            String query = "";
            Stat stat = zookeeper.exists(path, false);
            if (stat != null) {
                byte[] data = zookeeper.getData(path, false, stat);
                if (data != null && data.length > 0) {
                    query = "?" + new String(data);
                }
            }
            urls.add(URL.valueOf(URL.decode(provider + query)));
        }
        return urls;
    }

    private class NotifyWatcher implements Watcher {
        
        private final URL url;

        private final NotifyListener listener;

        public NotifyWatcher(URL url, NotifyListener listener) {
            this.url = url;
            this.listener = listener;
        }

        public void process(WatchedEvent event) {
            if (event.getType() == EventType.NodeChildrenChanged) {
                listener.notify(lookup(url));
            } else if (event.getType() == EventType.NodeDataChanged) {
                listener.notify(lookup(url));
            }
        }

    }

}
