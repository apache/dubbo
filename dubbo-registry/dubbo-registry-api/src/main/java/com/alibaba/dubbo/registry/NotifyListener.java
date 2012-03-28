/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry;

import java.util.List;

import com.alibaba.dubbo.common.URL;

/**
 * NotifyListener. (API, Prototype, ThreadSafe)
 * 
 * @see com.alibaba.dubbo.registry.RegistryService#subscribe(URL, NotifyListener)
 * @author william.liangf
 */
public interface NotifyListener {

    /**
     * 当收到服务变更通知时触发。
     * 
     * 通知需处理契约：<br>
     * 1. 总是以服务接口为维度全量通知，即不会通知一个服务的部分数据，用户不需要对比上一次通知结果
     * 2. 允许不同类型的数据分开通知，比如：providers, consumers, routes, overrides，允许只通知其中一种类型，但该类型的数据必须是全量的，不是增量的。
     * 3. 如果一种类型的数据为空，需通知一个noprovider, noconsumer, noroute, nooverride协议的标识性URL数据。
     * 4. 通知者(即注册中心实现)需保证通知的顺序，比如：单线程推送，队列串行化，带版本对比。
     * @param urls 含义同{@link com.alibaba.dubbo.registry.RegistryService#register(URL)}的url参数。
     */
    void notify(List<URL> urls);

}