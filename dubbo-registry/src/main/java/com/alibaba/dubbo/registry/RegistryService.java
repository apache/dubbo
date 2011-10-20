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
 * RegistryService
 * 
 * @author william.liangf
 */
public interface RegistryService {
    
    /**
     * 注册服务
     * 
     * @param url 服务提供者地址
     */
    void register(URL url);
    
    /**
     * 取消注册服务
     * 
     * @param url 服务提供者地址
     */
    void unregister(URL url);
    
    /**
     * 订阅服务
     * 
     * @param url 服务查询键值对，如：version=1.0.0&application=kylin
     * @param listener 服务变更事件监听器
     */
    void subscribe(URL url, NotifyListener listener);
    
    /**
     * 取消订阅服务
     * 
     * @param url 服务查询键值对，如：version=1.0.0&application=kylin
     * @param listener 服务变更事件监听器
     */
    void unsubscribe(URL url, NotifyListener listener);
    
    /**
     * 查询服务
     * 
     * @param url 服务查询键值对
     * @return 服务提供者者列表
     */
    List<URL> lookup(URL url);
    
}