/*
 * Copyright 1999-2012 Alibaba Group.
 *    
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *    
 *        http://www.apache.org/licenses/LICENSE-2.0
 *    
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.alibaba.dubbo.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Exporter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
class LocalServiceStore {

    private static final LocalServiceStore INSTANCE = new LocalServiceStore();

    public static LocalServiceStore getInstance() {
        return INSTANCE;
    }

    private LocalServiceStore() {
    }

    private ConcurrentMap<String, Exporter<?>> exportMap = 
            new ConcurrentHashMap<String, Exporter<?>>();

    public void register(URL url, Exporter<?> exporter) {
        if (url != null && exporter != null) {
            exportMap.putIfAbsent(url.getServiceKey(), exporter);
        }
    }

    public boolean isRegistered(String key) {
        return StringUtils.isNotEmpty(key) && exportMap.containsKey(key);
    }

    public void unregister(URL url) {
        if (url != null) {
            Exporter<?> exporter = exportMap.get(url.getServiceKey());
            if (exporter != null) {
                try {
                    exporter.unexport();
                } catch (Throwable e) { /* ignore */ }
            }
        }
    }

    public static String serviceKey(Map<String, String> map) {
        StringBuilder sb = new StringBuilder(32);
        String group = map.get(Constants.GROUP_KEY);
        String inf = map.get(Constants.INTERFACE_KEY);
        String version = map.get(Constants.VERSION_KEY);
        if (StringUtils.isNotEmpty(group)) {
            sb.append(group)
                    .append("/");
        }
        sb.append(inf);
        if (StringUtils.isNotEmpty(version)) {
            sb.append(":")
                    .append(version);
        }
        return sb.toString();
    }

}
