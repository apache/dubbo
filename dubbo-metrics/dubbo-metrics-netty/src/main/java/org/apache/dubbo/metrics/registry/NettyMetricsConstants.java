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
package org.apache.dubbo.metrics.registry;

import org.apache.dubbo.metrics.model.key.MetricsKey;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_CHUNK_SIZE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_DIRECT_ARENAS_NUM;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_DIRECT_MEMORY_USED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_HEAP_ARENAS_NUM;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_HEAP_MEMORY_USED;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_NORMAL_CACHE_SIZE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_PINNED_DIRECT_MEMORY;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_PINNED_HEAP_MEMORY;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_SMALL_CACHE_SIZE;
import static org.apache.dubbo.metrics.model.key.MetricsKey.NETTY_ALLOCATOR_THREAD_LOCAL_CACHES_NUM;

public interface NettyMetricsConstants {

    // App-level
    List<MetricsKey> APP_LEVEL_KEYS = Arrays.asList(
            NETTY_ALLOCATOR_HEAP_MEMORY_USED,
            NETTY_ALLOCATOR_DIRECT_MEMORY_USED,
            NETTY_ALLOCATOR_PINNED_DIRECT_MEMORY,
            NETTY_ALLOCATOR_PINNED_HEAP_MEMORY,
            NETTY_ALLOCATOR_HEAP_ARENAS_NUM,
            NETTY_ALLOCATOR_DIRECT_ARENAS_NUM,
            NETTY_ALLOCATOR_NORMAL_CACHE_SIZE,
            NETTY_ALLOCATOR_SMALL_CACHE_SIZE,
            NETTY_ALLOCATOR_THREAD_LOCAL_CACHES_NUM,
            NETTY_ALLOCATOR_CHUNK_SIZE);
}
