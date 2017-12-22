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
package com.alibaba.dubbo.config.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO Comment of MockRegistry
 */
public class MockRegistry implements Registry {

    static URL subscribedUrl = new URL("null", "0.0.0.0", 0);

    public static URL getSubscribedUrl() {
        return subscribedUrl;
    }

    /* 
     * @see com.alibaba.dubbo.common.Node#getUrl()
     */
    public URL getUrl() {
        return null;
    }

    /* 
     * @see com.alibaba.dubbo.common.Node#isAvailable()
     */
    public boolean isAvailable() {
        return true;
    }

    /* 
     * @see com.alibaba.dubbo.common.Node#destroy()
     */
    public void destroy() {

    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#register(com.alibaba.dubbo.common.URL)
     */
    public void register(URL url) {

    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#unregister(com.alibaba.dubbo.common.URL)
     */
    public void unregister(URL url) {

    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#subscribe(com.alibaba.dubbo.common.URL, com.alibaba.dubbo.registry.NotifyListener)
     */
    public void subscribe(URL url, NotifyListener listener) {
        this.subscribedUrl = url;
        List<URL> urls = new ArrayList<URL>();

        urls.add(url.setProtocol("mockprotocol")
                .removeParameter(Constants.CATEGORY_KEY)
                .addParameter(Constants.METHODS_KEY, "sayHello"));

        listener.notify(urls);
    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#unsubscribe(com.alibaba.dubbo.common.URL, com.alibaba.dubbo.registry.NotifyListener)
     */
    public void unsubscribe(URL url, NotifyListener listener) {

    }

    /* 
     * @see com.alibaba.dubbo.registry.RegistryService#lookup(com.alibaba.dubbo.common.URL)
     */
    public List<URL> lookup(URL url) {
        return null;
    }

}