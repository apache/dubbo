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
package org.apache.dubbo.remoting.zookeeper.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;

import java.util.Set;

/**
 * 2019/1/10
 */
public class ZookeeperClientData {
    public ZookeeperClientData(ZookeeperClient zookeeperClient, Set<URL> originalURLs) {
        this.zookeeperClient = zookeeperClient;
        this.originalURLs = originalURLs;
    }

    public void fillOriginalURL(URL url) {
        originalURLs.add(url);
    }

    ZookeeperClient zookeeperClient;
    Set<URL> originalURLs;

    public ZookeeperClient getZookeeperClient() {
        return zookeeperClient;
    }

    @Override
    public String toString() {
        return "ZookeeperClientData{" +
                "zookeeperClient=" + zookeeperClient +
                ", originalURLs=" + originalURLs +
                '}';
    }


}
