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
package org.apache.dubbo.remoting.zookeeper;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.RemotingConstants.BACKUP_KEY;

/**
 * AbstractZookeeperTransporter is abstract implements of ZookeeperTransporter.
 * <p>
 * If you want to extends this, implements createZookeeperClient.
 */
public abstract class AbstractZookeeperTransporter implements ZookeeperTransporter {
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTransporter.class);
    private final Map<ZookeeperClient, Set<String>> zookeeperApplicationMap = new ConcurrentHashMap<>();
    private final Map<String, ZookeeperClient> zookeeperClientMap = new ConcurrentHashMap<>();

    /**
     * share connnect for registry, metadata, etc..
     * <p>
     * Make sure the connection is connected.
     *
     * @param url
     * @return
     */
    @Override
    public ZookeeperClient connect(URL url) {
        ZookeeperClient zookeeperClient;
        // address format: {[username:password@]address}
        List<String> addressList = getURLBackupAddress(url);
        String application = url.getParameter(APPLICATION_KEY, StringUtils.EMPTY_STRING);
        // avoid creating too many connections， so add lock
        synchronized (zookeeperApplicationMap) {
            if ((zookeeperClient = fetchAndUpdateZookeeperClientCache(addressList, application)) != null &&
                    zookeeperClient.isConnected()) {
                logger.info("find valid zookeeper client from the cache for address: " + url);
                return zookeeperClient;
            }

            zookeeperClient = createZookeeperClient(url);
            logger.info("No valid zookeeper client found from cache, therefore create a new client for url. " + url);
            writeToClientMap(addressList, zookeeperClient);
            Set<String> appSet = new HashSet<>();
            appSet.add(application);
            zookeeperApplicationMap.put(zookeeperClient, appSet);            
        }
        return zookeeperClient;
    }

    /**
     * close the zookeeper client if it is no longer used by any application.
     * 
     * @param zookeeperClient
     * @param application the application which is going to be destroyed. 
     */
    @Override
    public void close(ZookeeperClient zookeeperClient, String application) {
        synchronized (zookeeperApplicationMap) {
            Set<String> appSet = zookeeperApplicationMap.get(zookeeperClient);
            if (appSet == null) {
                // zookeeperClient might be closed by other zookeeper registry of the same application.
                if (zookeeperClient.isConnected()) {
                    logger.warn("Application: " + application + " associated with the alive client: "
                            + zookeeperClient.getUrl() + " is not cached in zookeeperApplicationMap.");
                    zookeeperClient.close();
                }
                return;
            }

            if (application == null) {
                application = StringUtils.EMPTY_STRING;
            }
            if (appSet.remove(application)) {
                if (appSet.isEmpty()) {
                    if (zookeeperClient.isConnected()) {
                        zookeeperClient.close();                        
                    }
                    clearZookeeperClientCache(zookeeperClient);
                    zookeeperApplicationMap.remove(zookeeperClient);
                }
            }
        }
    }

    /**
     * close zookeeper clients that are no longer used by any application.
     * 
     * @param application the application which is going to be destroyed.
     */
    @Override
    public void close(String application) {
        synchronized (zookeeperApplicationMap) {
            if (application == null) {
                application = StringUtils.EMPTY_STRING;
            }
            Iterator<Entry<ZookeeperClient, Set<String>>> iterator = zookeeperApplicationMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<ZookeeperClient, Set<String>> entry = iterator.next();
                Set<String> appSet = entry.getValue();
                if (appSet == null) {
                    if (entry.getKey().isConnected()) {
                        logger.warn("Applications associated with the alive client: " + entry.getKey().getUrl()
                                + " are not cached in zookeeperApplicationMap.");
                        entry.getKey().close();
                    }
                    clearZookeeperClientCache(entry.getKey());
                    iterator.remove();
                    continue;
                }
                if (appSet.remove(application)) {
                    if (appSet.isEmpty()) {
                        if (entry.getKey().isConnected()) {
                            entry.getKey().close();
                        }
                        clearZookeeperClientCache(entry.getKey());
                        iterator.remove();
                    }
                }
            }
        }
    }

    /**
     * @param url the url that will create zookeeper connection .
     *            The url in AbstractZookeeperTransporter#connect parameter is rewritten by this one.
     *            such as: zookeeper://127.0.0.1:2181/org.apache.dubbo.remoting.zookeeper.ZookeeperTransporter
     * @return
     */
    protected abstract ZookeeperClient createZookeeperClient(URL url);

    /**
     * get the ZookeeperClient from cache, the ZookeeperClient must be connected.
     * <p>
     * It is not private method for unit test.
     *
     * @param addressList
     * @return
     */
    public ZookeeperClient fetchAndUpdateZookeeperClientCache(List<String> addressList, String application) {

        ZookeeperClient zookeeperClient = null;
        for (String address : addressList) {
            if ((zookeeperClient = zookeeperClientMap.get(address)) != null) {
                if (zookeeperClient.isConnected()) {
                    break;
                } else {
                    zookeeperClientMap.remove(address);
                    zookeeperApplicationMap.remove(zookeeperClient);
                }
            }
        }
        if (zookeeperClient != null && zookeeperClient.isConnected()) {
            writeToClientMap(addressList, zookeeperClient);
            Set<String> appSet = zookeeperApplicationMap.get(zookeeperClient);
            if (appSet == null) {
                logger.warn("No application is cached in zookeeperApplicationMap for the alive client: "
                        + zookeeperClient.getUrl());
                appSet = new HashSet<>();
                zookeeperApplicationMap.put(zookeeperClient, appSet);
            }
            appSet.add(application);
        }
        return zookeeperClient;
    }

    private void clearZookeeperClientCache(ZookeeperClient zookeeperClient) {
        Iterator<Entry<String, ZookeeperClient>> iterator = zookeeperClientMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, ZookeeperClient> entry = iterator.next();
            if (entry.getValue() == zookeeperClient) {
                iterator.remove();
            }
        }
    }

    /**
     * get all zookeeper urls (such as :zookeeper://127.0.0.1:2181?127.0.0.1:8989,127.0.0.1:9999)
     *
     * @param url such as:zookeeper://127.0.0.1:2181?127.0.0.1:8989,127.0.0.1:9999
     * @return such as 127.0.0.1:2181,127.0.0.1:8989,127.0.0.1:9999
     */
    @SuppressWarnings("unchecked")
    public List<String> getURLBackupAddress(URL url) {
        List<String> addressList = new ArrayList<String>();
        addressList.add(url.getAddress());
        addressList.addAll(url.getParameter(BACKUP_KEY, Collections.EMPTY_LIST));

        String authPrefix = null;
        if (StringUtils.isNotEmpty(url.getUsername())) {
            StringBuilder buf = new StringBuilder();
            buf.append(url.getUsername());
            if (StringUtils.isNotEmpty(url.getPassword())) {
                buf.append(':');
                buf.append(url.getPassword());
            }
            buf.append('@');
            authPrefix = buf.toString();
        }

        if (StringUtils.isNotEmpty(authPrefix)) {
            List<String> authedAddressList = new ArrayList<>(addressList.size());
            for (String addr : addressList) {
                authedAddressList.add(authPrefix + addr);
            }
            return authedAddressList;
        }


        return addressList;
    }

    /**
     * write address-ZookeeperClient relationship to Map
     *
     * @param addressList
     * @param zookeeperClient
     */
    void writeToClientMap(List<String> addressList, ZookeeperClient zookeeperClient) {
        for (String address : addressList) {
            zookeeperClientMap.put(address, zookeeperClient);
        }
    }

    /**
     * redefine the url for zookeeper. just keep protocol, username, password, host, port, and individual parameter.
     *
     * @param url
     * @return
     */
    URL toClientURL(URL url) {
        Map<String, String> parameterMap = new HashMap<>();
        // for CuratorZookeeperClient
        if (url.getParameter(TIMEOUT_KEY) != null) {
            parameterMap.put(TIMEOUT_KEY, url.getParameter(TIMEOUT_KEY));
        }
        if (url.getParameter(BACKUP_KEY) != null) {
            parameterMap.put(BACKUP_KEY, url.getParameter(BACKUP_KEY));
        }

        return new ServiceConfigURL(url.getProtocol(), url.getUsername(), url.getPassword(), url.getHost(), url.getPort(),
                ZookeeperTransporter.class.getName(), parameterMap);
    }

    /**
     * for unit test
     *
     * @return
     */
    public Map<String, ZookeeperClient> getZookeeperClientMap() {
        return zookeeperClientMap;
    }
}
