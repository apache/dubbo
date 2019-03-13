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

package org.apache.dubbo.configcenter.support.etcd;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.ConfigurationListener;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.remoting.etcd.EtcdClient;
import org.apache.dubbo.remoting.etcd.jetcd.JEtcdClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.Constants.CONFIG_NAMESPACE_KEY;
import static org.apache.dubbo.common.Constants.PATH_SEPARATOR;

/**
 * The etcd implementation of {@link DynamicConfiguration}
 */
public class EtcdDynamicConfiguration implements DynamicConfiguration {

    /**
     * The final root path would be: /$NAME_SPACE/config
     */
    private String rootPath;

    /**
     * The etcd client
     */
    private final EtcdClient etcdClient;

    /**
     * The map store the key to {@link EtcdConfigListener} mapping
     */
    private final ConcurrentMap<String, EtcdConfigListener> watchListenerMap;

    EtcdDynamicConfiguration(URL url) {
        rootPath = "/" + url.getParameter(CONFIG_NAMESPACE_KEY, DEFAULT_GROUP) + "/config";
        etcdClient = new JEtcdClient(url);
        watchListenerMap = new ConcurrentHashMap<>();
    }

    @Override
    public void addListener(String key, String group, ConfigurationListener listener) {
        String normalizedKey = convertKey(key);
        etcdClient.addWatchListener(normalizedKey, new EtcdConfigListener(listener));
    }

    @Override
    public void removeListener(String key, String group, ConfigurationListener listener) {

    }

    // TODO Abstract the logic into super class
    @Override
    public String getConfig(String key, String group, long timeout) throws IllegalStateException {
        if (StringUtils.isNotEmpty(group)) {
            key = group + PATH_SEPARATOR + key;
        } else {
            int i = key.lastIndexOf(".");
            key = key.substring(0, i) + PATH_SEPARATOR + key.substring(i + 1);
        }
        return (String) getInternalProperty(rootPath + PATH_SEPARATOR + key);
    }

    @Override
    public Object getInternalProperty(String key) {
        return etcdClient.getKVValue(key);
    }


    private String convertKey(String key) {
        int index = key.lastIndexOf('.');
        return rootPath + PATH_SEPARATOR + key.substring(0, index) + PATH_SEPARATOR + key.substring(index + 1);
    }
}
