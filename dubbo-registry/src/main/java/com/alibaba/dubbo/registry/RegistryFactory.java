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

import com.alibaba.dubbo.common.Adaptive;
import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;

/**
 * RegistryFactory. (SPI, Singleton, ThreadSafe)
 * 
 * NOTE: RegistryFactory should <strong>NOT</strong> have default implement.
 * 
 * @see com.alibaba.dubbo.registry.support.AbstractRegistryFactory
 * @author william.liangf
 */
@Extension("dubbo")
public interface RegistryFactory {

    /**
     * get registry.
     * 
     * @param url registry url
     * @return registry
     */
    @Adaptive({"protocol"})
    Registry getRegistry(URL url);

}