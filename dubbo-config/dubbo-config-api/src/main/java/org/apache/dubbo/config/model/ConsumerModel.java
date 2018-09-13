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
package org.apache.dubbo.config.model;

import org.apache.dubbo.config.ReferenceConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ConsumerModel {
    private ReferenceConfig metadata;
    private Object proxyObject;
    private String serviceName;

    private final Map<Method, ConsumerMethodModel> methodModels = new IdentityHashMap<Method, ConsumerMethodModel>();

    public ConsumerModel(String serviceName,ReferenceConfig metadata, Object proxyObject, Method[] methods) {
        this.serviceName = serviceName;
        this.metadata = metadata;
        this.proxyObject = proxyObject;

        if (proxyObject != null) {
            for (Method method : methods) {
                methodModels.put(method, new ConsumerMethodModel(method, metadata));
            }
        }
    }

    /**
     * Return service metadata for consumer
     *
     * @return service metadata
     */
    public ReferenceConfig getMetadata() {
        return metadata;
    }

    public Object getProxyObject() {
        return proxyObject;
    }

    /**
     * Return method model for the given method on consumer side
     *
     * @param method method object
     * @return method model
     */
    public ConsumerMethodModel getMethodModel(Method method) {
        return methodModels.get(method);
    }

    /**
     * Return all method models for the current service
     *
     * @return method model list
     */
    public List<ConsumerMethodModel> getAllMethods() {
        return new ArrayList<ConsumerMethodModel>(methodModels.values());
    }

    public String getServiceName() {
        return serviceName;
    }
}
