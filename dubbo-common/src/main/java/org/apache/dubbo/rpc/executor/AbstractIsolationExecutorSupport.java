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
package org.apache.dubbo.rpc.executor;

import org.apache.dubbo.common.ServiceKey;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.resource.GlobalResourcesRepository;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public abstract class AbstractIsolationExecutorSupport implements ExecutorSupport {
    private final URL url;
    private final ExecutorRepository executorRepository;
    private final Map<String, Executor> executorMap;

    public AbstractIsolationExecutorSupport(URL url) {
        this.url = url;
        this.executorRepository = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel());
        this.executorMap = new HashMap<>();
        GlobalResourcesRepository.getInstance().registerDisposable(this::destroy);
    }

    public Executor getExecutor(Object data) {

        ServiceKey serviceKey = getServiceKey(data);
        if (!isValid(serviceKey)) {
            return null;
        }
        String interfaceName = serviceKey.getInterfaceName();
        String version = serviceKey.getVersion();
        String group = serviceKey.getGroup();
        String cachedKey = URL.buildKey(interfaceName, group, version);
        if (executorMap.containsKey(cachedKey)) {
            return executorMap.get(cachedKey);
        }

        synchronized (this) {
            if (executorMap.containsKey(cachedKey)) {
                return executorMap.get(cachedKey);
            }
            Map<String, String> parameters = url.getParameters();
            parameters.put(GROUP_KEY, group);
            parameters.put(INTERFACE_KEY, interfaceName);
            parameters.put(VERSION_KEY, version);
            ServiceConfigURL tmpURL = new ServiceConfigURL(url.getProtocol(), url.getHost(), url.getPort(), interfaceName, parameters);
            ExecutorService executor = executorRepository.getExecutor(tmpURL);
            executorMap.put(cachedKey, executor);
            return executor;
        }
    }

    public synchronized void destroy() {
        executorMap.clear();
    }

    private boolean isValid(ServiceKey serviceKey) {
        return serviceKey != null && StringUtils.isNotEmpty(serviceKey.getInterfaceName());
    }

    protected abstract ServiceKey getServiceKey(Object data);
}
