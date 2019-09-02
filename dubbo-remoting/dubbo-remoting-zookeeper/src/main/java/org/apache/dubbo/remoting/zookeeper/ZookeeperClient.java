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

import java.util.List;

/**
 * 客户端接口 默认不配置zkclient的情况下 使用 curator
 */
public interface ZookeeperClient {

    /**
     * 创建节点
     * @param path
     * @param ephemeral
     */
    void create(String path, boolean ephemeral);

    /**
     * 删除节点
     * @param path
     */
    void delete(String path);

    /**
     *
     * @param path
     * @return
     */
    List<String> getChildren(String path);

    /**
     * 添加 ChildListener
     * @param path
     * @param listener
     * @return
     */
    List<String> addChildListener(String path, ChildListener listener);

    void removeChildListener(String path, ChildListener listener);

    void addStateListener(StateListener listener);

    void removeStateListener(StateListener listener);

    /**
     * 是否链接
     * @return
     */
    boolean isConnected();

    /**
     * 关闭
     */
    void close();

    /**
     * 注册中心的url
     * @return
     */
    URL getUrl();

    void create(String path, String content, boolean ephemeral);

    String getContent(String path);

}
