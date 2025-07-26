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
package com.alibaba.dubbo.registry;

import com.alibaba.dubbo.common.URL;

import java.util.List;

/**
 * NotifyListener. (API, Prototype, ThreadSafe)
 *
 * @see com.alibaba.dubbo.registry.RegistryService#subscribe(URL, NotifyListener)
 * 通知监听器，当收到服务变更通知时触发
 */
public interface NotifyListener {

    /**
     * 当收到服务变更通知时触发
     * 1.总是以服务接口和数据类型为维度全量通知，不会通知一个服务的同类型的部分数据
     * 2.订阅时的第一次通知，必须是一个服务的所有类型数据的全量通知
     * 3.通途变更时，运行不同类型的数据分开通知，比如：providers, consumers, routers, overrides，允许只通知其中一种类型，但该类型的数据必须是全量的，不是增量的。
     * 4.如果一种类型的数据为空，需通知一个empty协议并带category参数的标识性URL数据。
     * 5.通知者(注册中心实现)需保证通知的顺序，
     * @param urls
     */
    void notify(List<URL> urls);

}