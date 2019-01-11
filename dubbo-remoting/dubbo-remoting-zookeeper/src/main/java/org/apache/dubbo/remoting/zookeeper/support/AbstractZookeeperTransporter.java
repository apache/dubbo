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
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2019/1/10
 */
public abstract class AbstractZookeeperTransporter implements ZookeeperTransporter {
    static final Logger logger = LoggerFactory.getLogger(ZookeeperTransporter.class);
    Map<String, ZookeeperClientData> zookeeperClientMap = new ConcurrentHashMap<>();

    /**
     * share connnect for registry, metadata, etc..
     *
     * @param url
     * @return
     */
    public ZookeeperClient connect(URL url) {
        ZookeeperClientData clientData;
        List<String> addressList = getURLBackupAddress(url);
        // The field define the zookeeper server , including protocol, host, port, username, password
        if ((clientData = fetchAndUpdateZookeeperClientCache(url, addressList)) != null) {
            logger.info("ZookeeperTransporter.connnect result from map:" + clientData);
            return clientData.getZookeeperClient();
        }
        ZookeeperClient zookeeperClient = null;
        // avoid creating too many connections， so add lock
        synchronized (zookeeperClientMap) {
            if ((clientData = fetchAndUpdateZookeeperClientCache(url, addressList)) != null) {
                logger.info("ZookeeperTransporter.connnect result from map2:" + clientData);
                return clientData.getZookeeperClient();
            }

            Set<URL> originalURLs = new ConcurrentHashSet<>(2);
            originalURLs.add(url);
            zookeeperClient = createZookeeperClient(createServerURL(url), originalURLs);

            ZookeeperClientData zookeeperClientData = new ZookeeperClientData(zookeeperClient, originalURLs);
            logger.info("ZookeeperTransporter.connnect result from new connection. " + clientData);

            writeToClientMap(addressList, zookeeperClientData);
        }
        return zookeeperClient;
    }

    /**
     * @param url          the url that will create zookeeper connection . The url in org.apache.dubbo.remoting.zookeeper.support.AbstractZookeeperTransporter#connect(org.apache.dubbo.common.URL) parameter is rewritten by this one.
     *                     such as: zookeeper://127.0.0.1:2181/org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter
     * @param originalURLs the source invoked url collections (it is org.apache.dubbo.remoting.zookeeper.support.AbstractZookeeperTransporter#connect paramter). such as : zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=metadatareport-local-xml-provider2&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=47418&specVersion=2.7.0-SNAPSHOT
     * @return
     */
    protected abstract ZookeeperClient createZookeeperClient(URL url, Set<URL> originalURLs);

    ZookeeperClientData fetchAndUpdateZookeeperClientCache(URL url, List<String> addressList) {

        ZookeeperClientData zookeeperClientData = null;
        for (String address : addressList) {
            if ((zookeeperClientData = zookeeperClientMap.get(address)) != null) {
                break;
            }
        }
        if (zookeeperClientData != null) {
            writeToClientMap(addressList, zookeeperClientData);
            zookeeperClientData.fillOriginalURL(url);
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
     * @param zookeeperClientData
     */
    void writeToClientMap(List<String> addressList, ZookeeperClientData zookeeperClientData) {
        for (String address : addressList) {
            zookeeperClientMap.put(address, zookeeperClientData);
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
        return new URL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(), ZookeeperTransporter.class.getName(), parameterMap);
    }

}
