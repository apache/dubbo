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
package org.apache.dubbo.rpc.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Consumer Model which is about subscribed services.
 */
public class ConsumerModel {
    private final ServiceMetadata serviceMetadata;
    private final Map<Method, ConsumerMethodModel> methodModels = new IdentityHashMap<Method, ConsumerMethodModel>();

    /**
     * This constructor create an instance of ConsumerModel and passed objects should not be null.
     * If service name, service instance, proxy object,methods should not be null. If these are null
     * then this constructor will throw {@link IllegalArgumentException}
     *
     * @param attributes Attributes of methods.
     * @param metadata
     */
    public ConsumerModel(Map<String, Object> attributes, ServiceMetadata metadata) {
        this.serviceMetadata = metadata;
        for (Method method : metadata.getServiceType().getMethods()) {
            methodModels.put(method, new ConsumerMethodModel(method, attributes));
        }
    }

    /**
     * @return serviceMetadata
     */
    public ServiceMetadata getServiceMetadata() {
        return serviceMetadata;
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
     * Return method model for the given method on consumer side
     *
     * @param method method object
     * @return method model
     */
    public ConsumerMethodModel getMethodModel(String method) {
        Optional<Map.Entry<Method, ConsumerMethodModel>> consumerMethodModelEntry = methodModels.entrySet().stream().filter(entry -> entry.getKey().getName().equals(method)).findFirst();
        return consumerMethodModelEntry.map(Map.Entry::getValue).orElse(null);
    }

    /**
     * @param method   metodName
     * @param argsType method arguments type
     * @return
     */
    public ConsumerMethodModel getMethodModel(String method, String[] argsType) {
        Optional<ConsumerMethodModel> consumerMethodModel = methodModels.entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals(method))
                .map(Map.Entry::getValue).filter(methodModel ->  Arrays.equals(argsType, methodModel.getParameterTypes()))
                .findFirst();
        return consumerMethodModel.orElse(null);
    }


    /**
     * @return
     */
    public Class<?> getServiceInterfaceClass() {
        return serviceMetadata.getServiceType();
    }

    /**
     * Return all method models for the current service
     *
     * @return method model list
     */
    public List<ConsumerMethodModel> getAllMethods() {
        return new ArrayList<ConsumerMethodModel>(methodModels.values());
    }

    /**
     * Return the proxy object used by called while creating instance of ConsumerModel
     *
     * @return
     */
    public Object getProxyObject() {
        return this.serviceMetadata.getTarget();
    }

    public String getServiceName() {
        return this.serviceMetadata.getServiceKey();
    }
}
