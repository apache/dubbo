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
package org.apache.dubbo.registry.client.selector;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The {@link ServiceInstanceSelector} implementation based on Random algorithm
 *
 * @see ThreadLocalRandom
 * @see ServiceInstanceSelector
 * @since 2.7.5
 */
public class RandomServiceInstanceSelector implements ServiceInstanceSelector {

    @Override
    public ServiceInstance select(URL registryURL, List<ServiceInstance> serviceInstances) {
        int size = serviceInstances.size();
        if (size < 1) {
            return null;
        }
        int index = size == 1 ? 0 : selectIndexRandomly(size);
        return serviceInstances.get(index);
    }

    protected int selectIndexRandomly(int size) {
        return ThreadLocalRandom.current().nextInt(size);
    }
}
