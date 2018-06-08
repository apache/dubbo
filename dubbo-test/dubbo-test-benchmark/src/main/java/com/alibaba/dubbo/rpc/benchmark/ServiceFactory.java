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
package com.alibaba.dubbo.rpc.benchmark;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract Service Factory,create custom nums Service
 */
public class ServiceFactory<T> {

    // Cache ExchangeClient
    private static ConcurrentHashMap<String, Object> services = new ConcurrentHashMap<String, Object>();
    String targetIP = null;
    int targetPort = 0;
    int connectTimeout = 0;
    int clientNums = 0;

    public String getTargetIP() {
        return targetIP;
    }

    public void setTargetIP(String targetIP) {
        this.targetIP = targetIP;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getClientNums() {
        return clientNums;
    }

    public void setClientNums(int clientNums) {
        this.clientNums = clientNums;
    }

    @SuppressWarnings("unchecked")
    public T get(final Class<T> cls) {
        String key = cls.getName();
        if (services.containsKey(key)) {
            return (T) services.get(key);

        } else {
            T service = createClient(cls, targetIP, targetPort, connectTimeout, clientNums);
            services.put(key, service);
            return (T) services.get(key);
        }
    }

    protected T createClient(Class<T> cls, String targetIP, int targetPort, int connectTimeout, int clientNums) {
        ReferenceConfig<T> referenceConfig = new ReferenceConfig<T>();
        referenceConfig.setInterface(cls);
        StringBuilder url = new StringBuilder();
        url.append("dubbo://");
        url.append(targetIP);
        url.append(":");
        url.append(targetPort);
        url.append("/");
        url.append(cls.getName());
        referenceConfig.setUrl(url.toString());
        // hardcode
        referenceConfig.setConnections(clientNums);
        ApplicationConfig application = new ApplicationConfig();
        application.setName("dubbo_consumer");
        referenceConfig.setApplication(application);
        referenceConfig.setTimeout(connectTimeout);
        return referenceConfig.get();
    }

}
