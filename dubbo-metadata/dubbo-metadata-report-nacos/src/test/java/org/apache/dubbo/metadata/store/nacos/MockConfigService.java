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
package org.apache.dubbo.metadata.store.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

public class MockConfigService implements ConfigService {
    @Override
    public String getConfig(String dataId, String group, long timeoutMs) throws NacosException {
        return null;
    }

    @Override
    public String getConfigAndSignListener(String dataId, String group, long timeoutMs, Listener listener) {
        return null;
    }

    @Override
    public void addListener(String dataId, String group, Listener listener) {

    }

    @Override
    public boolean publishConfig(String dataId, String group, String content) {
        return false;
    }

    @Override
    public boolean publishConfig(String dataId, String group, String content, String type) {
        return false;
    }

    @Override
    public boolean publishConfigCas(String dataId, String group, String content, String casMd5) {
        return false;
    }

    @Override
    public boolean publishConfigCas(String dataId, String group, String content, String casMd5, String type) {
        return false;
    }

    @Override
    public boolean removeConfig(String dataId, String group) {
        return false;
    }

    @Override
    public void removeListener(String dataId, String group, Listener listener) {

    }

    @Override
    public String getServerStatus() {
        return null;
    }

    @Override
    public void shutDown() {

    }
}
