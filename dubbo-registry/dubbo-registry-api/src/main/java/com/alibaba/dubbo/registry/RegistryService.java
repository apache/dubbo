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
 * 注册中心服务
 * 
 * 注册中心需处理契约：<br>
 * 1. 支持backup=10.20.153.10备选注册中心集群地址
 * 2. 支持URL上的protocol://username:password@host:port权限认证
 * 3. 支持file=registry.cache本地磁盘文件缓存
 * 4. 支持timeout=1000请求超时设置
 * 5. 支持session=60000会话超时或过期设置
 * 
 * @author william.liangf
 */
public interface RegistryService {

    /**
     * 注册服务.
     * 
     * 注册需处理契约：<br>
     * 1. 当URL设置了dynamic=false参数，则需持久存储，否则，当服务提供者出现断电等情况异常退出时，需自动删除。<br>
     * 2. 当注册中心重启，网络抖动，不能丢失数据，包括断线自动删除数据。<br>
     * 3. 当URL设置了category=overrides时，表示分类存储，缺省类别为providers，可按分类部分通知数据。<br>
     * 4. 当URL设置了check=false时，注册失败后不报错，在后台定时重试，否则抛出异常。<br>
     * 5. 允许URI相同但参数不同的URL并存，不能覆盖。<br>
     * 
     * @param url 服务提供者地址，如：dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    void register(URL url);

    /**
     * 取消注册服务.
     * 
     * 取消注册需处理契约：<br>
     * 1. 如果是dynamic=false的持久存储数据，找不到数据，则抛IllegalStateException，否则忽略。
     * 
     * @param url 服务提供者地址，如：dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    void unregister(URL url);

    /**
     * 订阅服务.
     * 
     * 订阅需处理契约：<br>
     * 1. 允许以interface,group,version,classifier作为条件查询，并允许星号通配，订阅所有接口的所有分组的所有版本，如：interface=com.alibaba.foo.BarService&group=foo&version=1.0.0&classifier=william，或：interface=*&group=*&version=*&classifier=*<br>
     * 2. 当注册中心重启，网络抖动，需自动恢复订阅请求。<br>
     * 3. 当URL设置了category=overrides，只通知指定分类的数据，多个分类用逗号分隔，并允许星号通配，表示订阅所有分类数据。<br>
     * 4. 当URL设置了check=false时，订阅失败后不报错，在后台定时重试<br>
     * 5. 允许URI相同但参数不同的URL并存，不能覆盖。<br>
     * 
     * @param url 服务查询键值对，如：subscribe://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener 服务变更事件监听器
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * 取消订阅服务.
     * 
     * @param url 服务查询键值对，如：subscribe://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener 服务变更事件监听器
     */
    void unsubscribe(URL url, NotifyListener listener);

    /**
     * 查询服务列表，与订阅服务相同，拉模式，只返回一次结果。
     * 
     * @param url 服务查询键值对，如：subscribe://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @return 服务列表
     */
    List<URL> lookup(URL url);

}