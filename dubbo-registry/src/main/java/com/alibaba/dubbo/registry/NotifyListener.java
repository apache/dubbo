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
import java.util.Map;

import com.alibaba.dubbo.common.URL;

/**
 * NotifyListener. (API, Prototype, ThreadSafe)
 * 
 * @see com.alibaba.dubbo.registry.Registry#subscribe(URL, NotifyListener)
 * @author william.liangf
 */
public interface NotifyListener {
    
    /**
     * 当收到服务变更通知时触发
     * @param urls 含义同{@link Registry#register(String, Map)}的urls参数。
     */
    void notify(List<URL> urls);
    
}