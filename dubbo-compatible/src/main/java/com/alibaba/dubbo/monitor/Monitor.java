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

package com.alibaba.dubbo.monitor;

import org.apache.dubbo.common.URL;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public interface Monitor extends org.apache.dubbo.monitor.Monitor {

    com.alibaba.dubbo.common.URL getUrl();

    void collect(com.alibaba.dubbo.common.URL statistics);

    List<com.alibaba.dubbo.common.URL> lookup(com.alibaba.dubbo.common.URL query);

    @Override
    default void collect(URL statistics) {
        this.collect(new com.alibaba.dubbo.common.URL(statistics));
    }

    @Override
    default List<URL> lookup(URL query) {
        return this.lookup(new com.alibaba.dubbo.common.URL(query)).stream().map(url -> url.getOriginalURL()).collect(Collectors.toList());
    }
}
