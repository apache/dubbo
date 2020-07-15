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
package org.apache.dubbo.config.mock;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;

/**
 * TODO Comment of MockRegistry
 */
public class MockRegistry implements Registry {

    static URL subscribedUrl = new URL("null", "0.0.0.0", 0);

    public static URL getSubscribedUrl() {
        return subscribedUrl;
    }

    /*
     * @see org.apache.dubbo.common.Node#getUrl()
     */
    public URL getUrl() {
        return null;
    }

    /*
     * @see org.apache.dubbo.common.Node#isAvailable()
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /*
     * @see org.apache.dubbo.common.Node#destroy()
     */
    @Override
    public void destroy() {

    }

    /*
     * @see org.apache.dubbo.registry.RegistryService#register(org.apache.dubbo.common.URL)
     */
    @Override
    public void register(URL url) {

    }

    /*
     * @see org.apache.dubbo.registry.RegistryService#unregister(org.apache.dubbo.common.URL)
     */
    @Override
    public void unregister(URL url) {

    }

    /*
     * @see org.apache.dubbo.registry.RegistryService#subscribe(org.apache.dubbo.common.URL, org.apache.dubbo.registry.NotifyListener)
     */
    @Override
    public void subscribe(URL url, NotifyListener listener) {
        this.subscribedUrl = url;
        List<URL> urls = new ArrayList<URL>();

        urls.add(url.setProtocol("mockprotocol")
                .removeParameter(CATEGORY_KEY)
                .addParameter(METHODS_KEY, "sayHello"));

        listener.notify(urls);
    }

    /*
     * @see org.apache.dubbo.registry.RegistryService#unsubscribe(org.apache.dubbo.common.URL, org.apache.dubbo.registry.NotifyListener)
     */
    @Override
    public void unsubscribe(URL url, NotifyListener listener) {

    }

    /*
     * @see org.apache.dubbo.registry.RegistryService#lookup(org.apache.dubbo.common.URL)
     */
    @Override
    public List<URL> lookup(URL url) {
        return null;
    }

}
