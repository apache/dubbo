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
package com.alibaba.dubbo.remoting.zookeeper;

/**
 * 状态监听器接口
 */
public interface StateListener {

    /**
     * 状态 - 已断开
     */
    int DISCONNECTED = 0;
    /**
     * 状态 - 已连接
     */
    int CONNECTED = 1;
    /**
     * 状态 - 已重连
     */
    int RECONNECTED = 2;

    /**
     * 状态变更回调
     *
     * @param connected 状态
     */
    void stateChanged(int connected);

}