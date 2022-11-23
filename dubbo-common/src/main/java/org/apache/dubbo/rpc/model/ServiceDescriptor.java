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

import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;

import java.util.List;
import java.util.Set;

/**
 * ServiceModel and ServiceMetadata are to some extent duplicated with each other. We should merge them in the future.
 */
public interface ServiceDescriptor {

    FullServiceDefinition getFullServiceDefinition(String serviceKey);

    String getInterfaceName();

    Class<?> getServiceInterfaceClass();

    Set<MethodDescriptor> getAllMethods();

    /**
     * Does not use Optional as return type to avoid potential performance decrease.
     *
     * @param methodName
     * @param params
     * @return
     */
    MethodDescriptor getMethod(String methodName, String params);

    /**
     * Does not use Optional as return type to avoid potential performance decrease.
     *
     * @param methodName
     * @param paramTypes
     * @return methodDescriptor
     */
    MethodDescriptor getMethod(String methodName, Class<?>[] paramTypes);

    List<MethodDescriptor> getMethods(String methodName);

}
