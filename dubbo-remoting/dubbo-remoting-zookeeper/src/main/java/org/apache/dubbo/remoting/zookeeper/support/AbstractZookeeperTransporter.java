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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractZookeeperTransporter is abstract implements of ZookeeperTransporter.
 * <p>
 * If you want to extends this, implements createZookeeperClient.
 */
public abstract class AbstractZookeeperTransporter implements ZookeeperTransporter {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTransporter.class);
    private final Map<String, ZookeeperClient> zookeeperClientMap = new ConcurrentHashMap<>();

    /**
     * share connnect for registry, metadata, etc..
     *
     * @param url
     * @return
     */
    public ZookeeperClient connect(URL url) {
        ZookeeperClient clientData;
        List<String> addressList = getURLBackupAddress(url);
        // The field define the zookeeper server , including protocol, host, port, username, password
        if ((clientData = fetchAndUpdateZookeeperClientCache(url, addressList)) != null) {
            logger.info("Get result from map for the first time when invoking zookeeperTransporter.connnect .");
            return clientData;
        }
        ZookeeperClient zookeeperClient = null;
        // avoid creating too many connections， so add lock
        synchronized (zookeeperClientMap) {
            if ((clientData = fetchAndUpdateZookeeperClientCache(url, addressList)) != null) {
                logger.info("Get result from map for the second time when invoking zookeeperTransporter.connnect .");
                return clientData;
            }

            zookeeperClient = createZookeeperClient(createServerURL(url));
            logger.info("Get result by creating new connection when invoking zookeeperTransporter.connnect .");
            writeToClientMap(addressList, zookeeperClient);
        }
        return zookeeperClient;
    }

    /**
     * @param url the url that will create zookeeper connection .
     *            The url in AbstractZookeeperTransporter#connect parameter is rewritten by this one.
     *            such as: zookeeper://127.0.0.1:2181/org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter
     * @return
     */
    protected abstract ZookeeperClient createZookeeperClient(URL url);

    ZookeeperClient fetchAndUpdateZookeeperClientCache(URL url, List<String> addressList) {

        ZookeeperClient zookeeperClientData = null;
        for (String address : addressList) {
            if ((zookeeperClientData = zookeeperClientMap.get(address)) != null) {
                break;
            }
        }
        if (zookeeperClientData != null) {
            writeToClientMap(addressList, zookeeperClientData);
        }
        return zookeeperClientData;
    }

    List<String> getURLBackupAddress(URL url) {
        List<String> addressList = new ArrayList<String>();
        addressList.add(url.getAddress());

        String[] backups = url.getParameter(Constants.BACKUP_KEY, new String[0]);
        addressList.addAll(Arrays.asList(backups));
        return addressList;
    }

    /**
     * write address-ZookeeperClient relationship to Map
     *
     * @param addressList
     * @param ZookeeperClient
     */
    void writeToClientMap(List<String> addressList, ZookeeperClient ZookeeperClient) {
        for (String address : addressList) {
            zookeeperClientMap.put(address, ZookeeperClient);
        }
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
        if (url.getParameter(Constants.BACKUP_KEY) != null) {
            parameterMap.put(Constants.BACKUP_KEY, url.getParameter(Constants.BACKUP_KEY));
        }
        return new URL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(),
                ZookeeperTransporter.class.getName(), parameterMap);
    }

    /**
     * for unit test
     *
     * @return
     */
    Map<String, ZookeeperClient> getZookeeperClientMap() {
        return zookeeperClientMap;
    }
}
