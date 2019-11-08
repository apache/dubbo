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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.List;

/**
 * Used to build metadata service url from ServiceInstance.
 *
 * @since 2.7.4
 */
@SPI
public interface MetadataServiceURLBuilder {

    /**
     * Build the {@link URL URLs} from the specified {@link ServiceInstance}
     *
     * @param serviceInstance {@link ServiceInstance}
     * @return TODO, usually, we generate one metadata url from one instance. There's no scenario to return a metadta url list.
     */
    List<URL> build(ServiceInstance serviceInstance);
}
