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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Invoker;

import java.util.concurrent.ConcurrentHashMap;

public class TriplePathResolver implements PathResolver {
    private static final Logger logger = LoggerFactory.getLogger(TriplePathResolver.class);

    private final ConcurrentHashMap<String, Invoker<?>> path2Invoker = new ConcurrentHashMap<>();

    @Override
    public void add(String path, Invoker<?> invoker) {
        path2Invoker.put(path, invoker);
    }

    @Override
    public Invoker<?> resolve(String path) {
        return path2Invoker.get(path);
    }

    @Override
    public void remove(String path) {
        path2Invoker.remove(path);
    }

    @Override
    public void destroy() {
        path2Invoker.clear();
    }

}
