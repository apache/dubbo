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
package org.apache.dubbo.common.threadpool.manager;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.executor.ExecutorSupport;
import org.apache.dubbo.rpc.executor.IsolationExecutorSupportFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;

import java.util.concurrent.ExecutorService;

import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_EXECUTOR;
import static org.apache.dubbo.common.constants.CommonConstants.THREAD_NAME_KEY;

/**
 * Thread pool isolation between services, that is, a service has its own thread pool and not interfere with each other
 */
public class IsolationExecutorRepository extends DefaultExecutorRepository {

    public IsolationExecutorRepository(ApplicationModel applicationModel) {
        super(applicationModel);
    }

    @Override
    protected URL setThreadNameIfAbsent(URL url, String executorCacheKey) {
        if (url.getParameter(THREAD_NAME_KEY) == null) {
            url = url.putAttribute(THREAD_NAME_KEY, "isolation-" + executorCacheKey);
        }
        return url;
    }

    @Override
    protected String getProviderKey(URL url) {
        if (url.getAttributes().containsKey(SERVICE_EXECUTOR)) {
            return url.getServiceKey();
        } else {
            return super.getProviderKey(url);
        }
    }

    @Override
    protected String getProviderKey(ProviderModel providerModel, URL url) {
        if (url.getAttributes().containsKey(SERVICE_EXECUTOR)) {
            return providerModel.getServiceKey();
        } else {
            return super.getProviderKey(url);
        }
    }

    @Override
    protected ExecutorService createExecutor(URL url) {
        Object executor = url.getAttributes().get(SERVICE_EXECUTOR);
        if (executor instanceof ExecutorService) {
            return (ExecutorService) executor;
        }
        return super.createExecutor(url);
    }

    @Override
    public ExecutorSupport getExecutorSupport(URL url) {
        return IsolationExecutorSupportFactory.getIsolationExecutorSupport(url);
    }
}
