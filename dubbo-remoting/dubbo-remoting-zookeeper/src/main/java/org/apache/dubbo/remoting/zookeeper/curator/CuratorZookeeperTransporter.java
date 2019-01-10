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
package org.apache.dubbo.remoting.zookeeper.curator;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CuratorZookeeperTransporter implements ZookeeperTransporter {

    static final Logger logger = LoggerFactory.getLogger(CuratorZookeeperTransporter.class);
    Map<String, ZookeeperClientData> zookeeperClientMap = new HashMap<String, ZookeeperClientData>();

    /**
     * share connnect for registry, metadata, etc..
     *
     * @param url
     * @return
     */
    @Override
    public ZookeeperClient connect(URL url) {
        ZookeeperClientData clientData;
        // The field define the zookeeper server , including protocol, host, port, username, password
        String serverIdentityString = url.toServerIdentityString();
        if ((clientData = zookeeperClientMap.get(serverIdentityString)) != null) {
            logger.info("ZookeeperTransporter.connnect result from map:" + clientData);
            clientData.fillOriginalURL(url);
            return clientData.zookeeperClient;
        }
        ZookeeperClient zookeeperClient = null;
        synchronized (zookeeperClientMap) {
            if ((clientData = zookeeperClientMap.get(serverIdentityString)) != null) {
                logger.info("ZookeeperTransporter.connnect result from map2:" + clientData);
                clientData.fillOriginalURL(url);
                return clientData.zookeeperClient;
            }

            Set<URL> originalURLs = new HashSet<>(2);
            originalURLs.add(url);
            zookeeperClient = new CuratorZookeeperClient(createServerURL(url), originalURLs);

            ZookeeperClientData zookeeperClientData = new ZookeeperClientData(zookeeperClient, originalURLs);
            logger.info("ZookeeperTransporter.connnect result from new connection. " + clientData);
            zookeeperClientMap.put(serverIdentityString, zookeeperClientData);
        }
        return zookeeperClient;
    }

    /**
     * redefine the url for zookeeper. just keep protocol, username, password, host, port, and individual parameter.
     *
     * @param url
     * @return
     */
    URL createServerURL(URL url) {
        Map<String, String> parameterMap = new HashMap<>();
        // for CuratorZookeeperClient
        if (url.getParameter(Constants.TIMEOUT_KEY) != null) {
            parameterMap.put(Constants.TIMEOUT_KEY, url.getParameter(Constants.TIMEOUT_KEY));
        }
        return new URL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), CuratorZookeeperTransporter.class.getName(), parameterMap);
    }

    static class ZookeeperClientData {

        public ZookeeperClientData(ZookeeperClient zookeeperClient, Set<URL> originalURLs) {
            this.zookeeperClient = zookeeperClient;
            this.originalURLs = originalURLs;
        }

        public void fillOriginalURL(URL url) {
            originalURLs.add(url);
        }

        ZookeeperClient zookeeperClient;
        Set<URL> originalURLs;

        @Override
        public String toString() {
            return "ZookeeperClientData{" +
                    "zookeeperClient=" + zookeeperClient +
                    ", originalURLs=" + originalURLs +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ZookeeperClientData)) return false;
            ZookeeperClientData that = (ZookeeperClientData) o;
            return Objects.equals(zookeeperClient, that.zookeeperClient) &&
                    Objects.equals(originalURLs, that.originalURLs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(zookeeperClient, originalURLs);
        }
    }

}
