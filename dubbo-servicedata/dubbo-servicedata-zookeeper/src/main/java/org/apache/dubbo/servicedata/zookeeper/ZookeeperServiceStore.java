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
package org.apache.dubbo.servicedata.zookeeper;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.servicedata.support.AbstractServiceStore;

import java.util.ArrayList;
import java.util.List;

/**
 * ZookeeperRegistry
 */
public class ZookeeperServiceStore extends AbstractServiceStore {

    private final static Logger logger = LoggerFactory.getLogger(ZookeeperServiceStore.class);

    private final static String DEFAULT_ROOT = "dubbo";

    private final static String TAG = "servicestore";

    private final String root;

    private final ZookeeperClient zkClient;

    public ZookeeperServiceStore(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT);
        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        this.root = group;
        zkClient = zookeeperTransporter.connect(url);
    }

    @Override
    protected void doPutService(URL url) {
        try {
            deletePath(url);
            url = url.removeParameters(Constants.BIND_IP_KEY, Constants.BIND_PORT_KEY, Constants.TIMESTAMP_KEY);
            zkClient.create(toUrlPathWithParameter(url), false);
        } catch (Throwable e) {
            logger.error("Failed to put " + url + " to zookeeper " + url + ", cause: " + e.getMessage(), e);
            throw new RpcException("Failed to put " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    private void deletePath(URL url) {
        String path = toCategoryPath(url);
        List<String> urlStrs = zkClient.getChildren(path);
        if (CollectionUtils.isEmpty(urlStrs)) {
            return;
        }
        for (String urlStr : urlStrs) {
            zkClient.delete(path + Constants.PATH_SEPARATOR + urlStr);
        }
    }

    @Override
    protected URL doPeekService(final URL url) {
        try {
            List<String> urlStrs = zkClient.getChildren((toCategoryPath(url)));
            List<URL> urls = new ArrayList<URL>();
            if (urlStrs != null && !urlStrs.isEmpty()) {
                for (String urlStr : urlStrs) {
                    urlStr = URL.decode(urlStr);
                    if (urlStr.contains("://")) {
                        urls.add(URL.valueOf(urlStr));
                    }
                }
            }
            return urls.isEmpty() ? null : urls.get(0);
        } catch (Throwable e) {
            logger.error("Failed to peek " + url + " to zookeeper " + url + ", cause: " + e.getMessage(), e);
            throw new RpcException("Failed to peek " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }

    private String toRootPath() {
        return root;
    }

    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    private String toCategoryPath(URL url) {
        String protocol = url.getParameter(Constants.SIDE_KEY);
        return toServicePath(url) + Constants.PATH_SEPARATOR + TAG + Constants.PATH_SEPARATOR + (protocol != null ? protocol : url.getProtocol());
    }

    private String toUrlPathWithParameter(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toParameterString());
    }

}
