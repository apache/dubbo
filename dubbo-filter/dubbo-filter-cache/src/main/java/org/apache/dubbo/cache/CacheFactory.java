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
package org.apache.dubbo.cache;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invocation;

/**
 * Interface needs to be implemented by all the cache store provider.Along with implementing <b>CacheFactory</b> interface
 * entry needs to be added in org.apache.dubbo.cache.CacheFactory file in a classpath META-INF sub directories.
 *
 * @see Cache
 */
@SPI("lru")
public interface CacheFactory {

    /**
     * CacheFactory implementation class needs to implement this return underlying cache instance for method against
     * url and invocation.
     * @param url
     * @param invocation
     * @return Instance of Cache containing cached value against method url and invocation.
     */
    @Adaptive("cache")
    Cache getCache(URL url, Invocation invocation);

}
