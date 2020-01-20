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
package org.apache.dubbo.config.spring.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;

import java.util.ArrayList;
import java.util.List;

public class MockRegistry implements Registry {

    private URL url;

    private List<URL> registered = new ArrayList<URL>();

    private List<URL> subscribered = new ArrayList<URL>();

    public MockRegistry(URL url) {
        if (url == null) {
            throw new NullPointerException();
        }
        this.url = url;
    }

    public List<URL> getRegistered() {
        return registered;
    }

    public List<URL> getSubscribered() {
        return subscribered;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void register(URL url) {
        registered.add(url);
    }

    @Override
    public void unregister(URL url) {
        registered.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        subscribered.add(url);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        subscribered.remove(url);
    }

    @Override
    public List<URL> lookup(URL url) {
        return null;
    }
}
