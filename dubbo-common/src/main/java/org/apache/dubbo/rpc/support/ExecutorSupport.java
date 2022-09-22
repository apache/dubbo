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
package org.apache.dubbo.rpc.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.resource.GlobalResourcesRepository;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.common.constants.CommonConstants.EXECUTOR_MANAGEMENT_MODE_DEFAULT;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * The role of ExecutorSupport is to obtain the executor(thread pool) of the service provider.
 * <br/>
 * 1.If EXECUTOR_MANAGEMENT_MODE is the default, it is obtained in the original way.(i.e. DefaultExecutorRepository)
 * <br/>
 * 2.If EXECUTOR_MANAGEMENT_MODE is the isolation, when multiple services are exposed, it means that there are multiple urls,
 * but only one url(ExecutorSupport#url) will remain in the end(Because the server will only open it once according to the address cache).
 * so the thread pool cannot be obtained according to this url. It is necessary to decode the tripleTuple according to
 * the request body(see ExecutorSupport#getTripleTuple method), so that we can obtain the isolation thread pool according to the tripleTuple.
 */
public abstract class ExecutorSupport {
    private Executor defaultExecutor;
    private final URL url;
    private final ExecutorRepository executorRepository;
    private final Map<String, Executor> executorMap;

    public ExecutorSupport(URL url) {
        this.url = url;
        this.executorRepository = ExecutorRepository.getInstance(url.getOrDefaultApplicationModel());
        String mode = ExecutorRepository.getMode(url.getOrDefaultApplicationModel());
        if (EXECUTOR_MANAGEMENT_MODE_DEFAULT.equals(mode)) {
            this.defaultExecutor = executorRepository.getExecutor(url);
        }
        this.executorMap = new HashMap<>();
        GlobalResourcesRepository.getInstance().registerDisposable(this::destroy);
    }

    public Executor getExecutor(Object data) {
        if (defaultExecutor != null) {
            return defaultExecutor;
        }
        TripleTuple tripleTuple = getTripleTuple(data);
        if (!isValid(tripleTuple)) {
            return null;
        }
        String interfaceName = tripleTuple.getInterfaceName();
        String version = tripleTuple.getVersion();
        String group = tripleTuple.getGroup();
        String serviceKey = URL.buildKey(interfaceName, group, version);
        if (executorMap.containsKey(serviceKey)) {
            return executorMap.get(serviceKey);
        }

        synchronized (this) {
            if (executorMap.containsKey(serviceKey)) {
                return executorMap.get(serviceKey);
            }
            Map<String, String> parameters = url.getParameters();
            parameters.put(GROUP_KEY, group);
            parameters.put(INTERFACE_KEY, interfaceName);
            parameters.put(VERSION_KEY, version);
            ServiceConfigURL tmpURL = new ServiceConfigURL(url.getProtocol(), url.getHost(), url.getPort(), interfaceName, parameters);
            ExecutorService executor = executorRepository.getExecutor(tmpURL);
            executorMap.put(serviceKey, executor);
            return executor;
        }
    }

    public synchronized void destroy() {
        executorMap.clear();
    }

    private boolean isValid(TripleTuple tuple) {
        return tuple != null && StringUtils.isNotEmpty(tuple.getInterfaceName());
    }

    protected abstract TripleTuple getTripleTuple(Object data);

}
