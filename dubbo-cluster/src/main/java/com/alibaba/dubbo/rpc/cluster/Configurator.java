/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.URL;

/**
 * Configurator. (SPI, Prototype, ThreadSafe)
 * 
 * @author william.liangf
 */
public interface Configurator extends Comparable<Configurator> {
    
    /**
     * get the configurator url.
     * 
     * @return configurator url.
     */
    URL getUrl();

    /**
     * Configure the provider url.
     * O
     * @param url - old rovider url.
     * @return new provider url.
     */
    URL configure(URL url);

}
