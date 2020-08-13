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
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.util.Collection;
import java.util.List;

/**
 * The interface to synthesize the subscribed {@link URL URLs}
 *
 * @since 2.7.5
 */
@SPI
public interface SubscribedURLsSynthesizer extends Prioritized {

    /**
     * Supports the synthesis of the subscribed {@link URL URLs} or not
     *
     * @param subscribedURL the original subscribed {@link URL} from the execution of`
     *                      {@link Registry#subscribe(URL, NotifyListener)} method
     * @return if supports, return <code>true</code>, or <code>false</code>
     */
    boolean supports(URL subscribedURL);

    /**
     * synthesize the subscribed {@link URL URLs}
     *
     * @param subscribedURL    the original subscribed {@link URL} from the execution of`
     *                         {@link Registry#subscribe(URL, NotifyListener)} method
     * @param serviceInstances
     * @return
     */
    List<URL> synthesize(URL subscribedURL, Collection<ServiceInstance> serviceInstances);
}
