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
package com.alibaba.dubbo.demo.registry;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;

public class RegistryServiceLocal implements RegistryService {
    RegistryService registryService;
    ConcurrentMap<URL, NotifyListener> listeners = new ConcurrentHashMap<URL, NotifyListener>();
    

    public RegistryServiceLocal(RegistryService registryService) {
        this.registryService = registryService;
    }

    public void register(URL url) {
        registryService.register(url);
    }

    public void unregister(URL url) {
        registryService.unregister(url);
    }

    public void subscribe(URL url, NotifyListener listener) {
        registryService.subscribe(url, listener);
        listeners.put(url, listener);
    }

    public void unsubscribe(URL url, NotifyListener listener) {
        registryService.unsubscribe(url, listener);
    }

    public List<URL> lookup(URL url) {
        return registryService.lookup(url);
    }
    
    public void ondisconnect(){
        for(URL url : listeners.keySet()){
            registryService.subscribe(url, listeners.get(url));
        }
    }
}