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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.common.utils.StringUtils;

import org.apache.commons.net.util.SubnetUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class SubnetUtil {
    public static final String TAG_SUBNETS_KEY = "tag.subnets";

    private static Map<String, List<SubnetUtils.SubnetInfo>> cellSubnets = new ConcurrentHashMap<>();
    protected static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected static final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    protected static final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    public static boolean isEmpty() {
        try {
            readLock.lock();
            return cellSubnets.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    public static void init(String content) {
        if (StringUtils.isBlank(content)) {
            return;
        }
        try {
            writeLock.lock();
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            cellSubnets = new HashMap<>();
            Map<String, List<String>> tmpPathSubnet = (Map<String, List<String>>) yaml.load(content);
            for (Map.Entry<String, List<String>> entry : tmpPathSubnet.entrySet()) {
                String path = entry.getKey();
                List<SubnetUtils.SubnetInfo> subnetInfos = cellSubnets.computeIfAbsent(path, f -> new ArrayList<SubnetUtils.SubnetInfo>());
                entry.getValue().forEach(e -> subnetInfos.add(new SubnetUtils(e.trim()).getInfo()));
            }
        } finally {
            writeLock.unlock();
        }
    }

    public static String getTagLevelByHost(String host) {
        try {
            readLock.lock();
            for (Map.Entry<String, List<SubnetUtils.SubnetInfo>> entry : cellSubnets.entrySet()) {
                for (SubnetUtils.SubnetInfo subnetInfo : entry.getValue()) {
                    if (subnetInfo.isInRange(host)) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }
}
