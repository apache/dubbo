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
package org.apache.dubbo.registry.support.cloud;

/**
 * Cloud {@link ServiceInstance Service} Registry
 *
 * @param <S> The subclass of {@link ServiceInstance}
 * @since 2.7.1
 */
public interface CloudServiceRegistry<S extends ServiceInstance> extends AutoCloseable {

    /**
     * Registers the {@link ServiceInstance}.
     *
     * @param serviceInstance The {@link ServiceInstance}
     */
    void register(S serviceInstance);

    /**
     * Deregisters the {@link ServiceInstance}
     *
     * @param serviceInstance The {@link ServiceInstance}
     */
    void deregister(S serviceInstance);


    /**
     * Test the specified {@link ServiceInstance} is healthy or not
     *
     * @param serviceInstance The {@link ServiceInstance}
     * @return <code>true</code> if the specified {@link ServiceInstance} is healthy
     */
    boolean isHealthy(S serviceInstance);

    /**
     * Is available or not
     *
     * @return <code>true</code> if current {@link CloudServiceRegistry} is available
     */
    boolean isAvailable();

}
