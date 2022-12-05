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
import org.apache.dubbo.common.config.configcenter.ConfigItem;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Common abstraction of Zookeeper client.
 */
public interface ZookeeperClient {

    /**
     * Create ZNode in Zookeeper.
     *
     * @param path path to ZNode
     * @param ephemeral specify create mode of ZNode creation. true - EPHEMERAL, false - PERSISTENT.
     * @param faultTolerant specify fault tolerance of ZNode creation.
     *                       true - ignore exception and recreate if is ephemeral, false - throw exception.
     */
    void create(String path, boolean ephemeral, boolean faultTolerant);

    /**
     * Delete ZNode.
     *
     * @param path path to ZNode
     */
    void delete(String path);

    List<String> getChildren(String path);

    List<String> addChildListener(String path, ChildListener listener);

    /**
     * Attach data listener to current Zookeeper client.
     *
     * @param path    directory. All children of path will be listened.
     * @param listener The data listener object.
     */
    void addDataListener(String path, DataListener listener);

    /**
     * Attach data listener to current Zookeeper client. The listener will be executed using the given executor.
     *
     * @param path    directory. All children of path will be listened.
     * @param listener The data listener object.
     * @param executor the executor that will execute the listener.
     */
    void addDataListener(String path, DataListener listener, Executor executor);

    /**
     * Detach data listener.
     *
     * @param path    directory. All listener of children of the path will be detached.
     * @param listener The data listener object.
     */
    void removeDataListener(String path, DataListener listener);

    void removeChildListener(String path, ChildListener listener);

    void addStateListener(StateListener listener);

    void removeStateListener(StateListener listener);

    /**
     * Check the Zookeeper client whether connected to server or not.
     *
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Close connection to Zookeeper server (cluster).
     */
    void close();

    URL getUrl();

    /**
     * Create or update ZNode in Zookeeper with content specified.
     *
     * @param path path to ZNode
     * @param content the content of ZNode
     * @param ephemeral specify create mode of ZNode creation. true - EPHEMERAL, false - PERSISTENT.
     */
    void createOrUpdate(String path, String content, boolean ephemeral);

    /**
     * CAS version to Create or update ZNode in Zookeeper with content specified.
     *
     * @param path path to ZNode
     * @param content the content of ZNode
     * @param ephemeral specify create mode of ZNode creation. true - EPHEMERAL, false - PERSISTENT.
     * @param ticket origin content version, if current version is not the specified version, throw exception
     */
    void createOrUpdate(String path, String content, boolean ephemeral, Integer ticket);

    /**
     * Obtain the content of a ZNode.
     *
     * @param path path to ZNode
     * @return content of ZNode
     */
    String getContent(String path);

    ConfigItem getConfigItem(String path);

    boolean checkExists(String path);

}
