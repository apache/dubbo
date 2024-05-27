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
package org.apache.dubbo.remoting.zookeeper.curator5;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_DESTROY_ZOOKEEPER;

public class ZookeeperClientManager {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(ZookeeperClientManager.class);
    private final Map<String, ZookeeperClient> zookeeperClientMap = new ConcurrentHashMap<>();
    private static final Map<ApplicationModel, ZookeeperClientManager> managerMap = new ConcurrentHashMap<>();

    /**
     * Use getInstance instead of get Instance
     * This method is made public solely for unit test purposes.
     */
    public ZookeeperClientManager() {}

    public static ZookeeperClientManager getInstance(ApplicationModel applicationModel) {
        if (!managerMap.containsKey(applicationModel) || managerMap.get(applicationModel) == null) {
            ZookeeperClientManager clientManager = new ZookeeperClientManager();
            applicationModel.addDestroyListener(m -> {
                // destroy zookeeper clients if any
                try {
                    clientManager.destroy();
                } catch (Exception e) {
                    logger.error(
                            TRANSPORT_FAILED_DESTROY_ZOOKEEPER,
                            "",
                            "",
                            "Error encountered while destroying ZookeeperTransporter: " + e.getMessage(),
                            e);
                }
            });
            managerMap.put(applicationModel, clientManager);
        }
        return managerMap.get(applicationModel);
    }

    public ZookeeperClient connect(URL url) {
        ZookeeperClient zookeeperClient;
        // address format: {[username:password@]address}
        List<String> addressList = getURLBackupAddress(url);
        // The field define the zookeeper server , including protocol, host, port, username, password
        if ((zookeeperClient = fetchAndUpdateZookeeperClientCache(addressList)) != null
                && zookeeperClient.isConnected()) {
            logger.info("find valid zookeeper client from the cache for address: " + url);
            return zookeeperClient;
        }
        // avoid creating too many connections， so add lock
        synchronized (zookeeperClientMap) {
            if ((zookeeperClient = fetchAndUpdateZookeeperClientCache(addressList)) != null
                    && zookeeperClient.isConnected()) {
                logger.info("find valid zookeeper client from the cache for address: " + url);
                return zookeeperClient;
            }

            zookeeperClient = new Curator5ZookeeperClient(url);
            logger.info("No valid zookeeper client found from cache, therefore create a new client for url. " + url);
            writeToClientMap(addressList, zookeeperClient);
        }
        return zookeeperClient;
    }

    public ZookeeperClient fetchAndUpdateZookeeperClientCache(List<String> addressList) {
        ZookeeperClient zookeeperClient = null;
        for (String address : addressList) {
            if ((zookeeperClient = zookeeperClientMap.get(address)) != null && zookeeperClient.isConnected()) {
                break;
            }
        }
        // mapping new backup address
        if (zookeeperClient != null && zookeeperClient.isConnected()) {
            writeToClientMap(addressList, zookeeperClient);
        }
        return zookeeperClient;
    }

    /**
     * get all zookeeper urls (such as zookeeper://127.0.0.1:2181?backup=127.0.0.1:8989,127.0.0.1:9999)
     *
     * @param url such as zookeeper://127.0.0.1:2181?backup=127.0.0.1:8989,127.0.0.1:9999
     * @return such as 127.0.0.1:2181,127.0.0.1:8989,127.0.0.1:9999
     */
    public List<String> getURLBackupAddress(URL url) {
        List<String> addressList = new ArrayList<>();
        addressList.add(url.getAddress());
        addressList.addAll(url.getParameter(RemotingConstants.BACKUP_KEY, Collections.emptyList()));

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
        if (url.getParameter(RemotingConstants.BACKUP_KEY) != null) {
            parameterMap.put(RemotingConstants.BACKUP_KEY, url.getParameter(RemotingConstants.BACKUP_KEY));
        }

        return new ServiceConfigURL(
                url.getProtocol(),
                url.getUsername(),
                url.getPassword(),
                url.getHost(),
                url.getPort(),
                ZookeeperClientManager.class.getName(),
                parameterMap);
    }

    /**
     * for unit test
     *
     * @return
     */
    public Map<String, ZookeeperClient> getZookeeperClientMap() {
        return zookeeperClientMap;
    }

    public void destroy() {
        // only destroy zk clients here
        for (ZookeeperClient client : zookeeperClientMap.values()) {
            client.close();
        }
        zookeeperClientMap.clear();
    }
}
