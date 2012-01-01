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
 * 1. 支持username=foo&password=bar权限认证
 * 2. 支持timeout=1000超时设置
 * 3. 支持backup=10.20.153.10备选注册中心地址
 * 
 * @author william.liangf
 */
public interface RegistryService {

    /**
     * 注册服务.
     * 
     * 注册需处理契约：<br>
     * 1. 当URL设置了check=false时，注册失败后不报错，在后台定时重试。<br>
     * 2. 允许写入route://协议的路由规则，且持久存储路由规则。<br>
     * 3. 允许URI相同但参数不同的URL并存，不能覆盖。<br>
     * 4. 当服务提供者出现断电等情况异常退出时，需自动删除当前提供者URL。<br>
     * 5. 当注册中心重启恢复时，需自动恢复注册数据。<br>
     * 
     * @param url 服务提供者地址，如：dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    void register(URL url);

    /**
     * 取消注册服务
     * 
     * @param url 服务提供者地址，如：dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    void unregister(URL url);

    /**
     * 订阅服务
     * 
     * 订阅需处理契约：<br>
     * 1. 当URL设置了check=false时，订阅失败后不报错，在后台定时重试<br>
     * 2. 当URL设置了admin=true时，结果不只包含服务提供者的URL和路由规则URL，还需包含所有服务订阅者的URL<br>
     * 3. 允许以interface,group,version,classifier作为条件查询，如：interface=com.alibaba.foo.BarService&group=foo&version=1.0.0&classifier=william<br>
     * 4. 允许星号通配，订阅所有接口的所有分组的所有版本，如：interface=*&group=*&version=*&classifier=* <br>
     * 5. 允许URI相同但参数不同的URL并存，不能覆盖。<br>
     * 6. 当服务消费者出现断电等情况异常退出时，需自动删除当前消费者URL。<br>
     * 7. 当注册中心重启恢复时，需自动恢复订阅请求。<br>
     * 
     * @param url 服务查询键值对，如：subscribe://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener 服务变更事件监听器
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * 取消订阅服务
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