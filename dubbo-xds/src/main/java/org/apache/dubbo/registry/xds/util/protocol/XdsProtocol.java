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
package org.apache.dubbo.registry.xds.util.protocol;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface XdsProtocol<T> {
    /**
     * Gets all {@link T resource} by the specified resource name.
     * For LDS, the {@param resourceNames} is ignored
     *
     * @param resourceNames specified resource name
     * @return resources, null if request failed
     */
    Map<String, T> getResource(Set<String> resourceNames);

    /**
     * Add a observer resource with {@link Consumer}
     *
     * @param resourceNames specified resource name
     * @param consumer      resource notifier, will be called when resource updated
     * @return requestId, used when resourceNames update with {@link XdsProtocol#updateObserve(long, Set)}
     */
    void observeResource(Set<String> resourceNames, Consumer<Map<String, T>> consumer, boolean isReConnect);
}
