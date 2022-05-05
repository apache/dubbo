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
package com.alibaba.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;

@Deprecated
public interface Configurator extends org.apache.dubbo.rpc.cluster.Configurator {
    /**
     * Get the configurator url.
     *
     * @return configurator url.
     */
    com.alibaba.dubbo.common.URL getUrl();

    /**
     * Configure the provider url.
     *
     * @param url - old provider url.
     * @return new provider url.
     */
    com.alibaba.dubbo.common.URL configure(com.alibaba.dubbo.common.URL url);

    @Override
    default URL configure(URL url) {
        return this.configure(new com.alibaba.dubbo.common.URL(url));
    }
}
