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
package com.alibaba.dubbo.registry.support;

import java.net.InetSocketAddress;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.Registry;

/**
 * 嵌入式注册中心实现，不开端口，只是map进行存储查询.不需要显示声明
 * 
 * @author chao.liuc
 * @author william.liangf
 */
public abstract class AbstractRegistry extends AbstractRegistryService implements Registry {

    private final URL registryUrl;
    
    public AbstractRegistry(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.registryUrl = url;
    }

    public URL getUrl() {
        return registryUrl;
    }

    public InetSocketAddress getLocalAddress() {
        return registryUrl.toInetSocketAddress();
    }

    public InetSocketAddress getRemoteAddress() {
        return registryUrl.toInetSocketAddress();
    }

    public boolean isAvailable() {
        return true;
    }

    public void destroy() {
    }

    public String toString() {
        return getUrl().toString();
    }

}