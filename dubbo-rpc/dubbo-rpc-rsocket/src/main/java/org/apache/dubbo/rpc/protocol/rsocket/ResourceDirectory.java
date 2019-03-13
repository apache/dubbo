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
package org.apache.dubbo.rpc.protocol.rsocket;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ResourceDirectory {

    private static AtomicLong idGen = new AtomicLong(1);

    private static ConcurrentHashMap<Long, Object> id2ResourceMap = new ConcurrentHashMap<Long, Object>();


    public static long mountResource(Object resource) {
        long id = idGen.getAndIncrement();
        id2ResourceMap.put(id, resource);
        return id;
    }

    public static Object unmountResource(long id) {
        return id2ResourceMap.get(id);
    }

    public static long mountMono(Mono mono) {
        long id = idGen.getAndIncrement();
        id2ResourceMap.put(id, mono);
        return id;
    }

    public static long mountFlux(Flux flux) {
        long id = idGen.getAndIncrement();
        id2ResourceMap.put(id, flux);
        return id;
    }

    public static Mono unmountMono(long id) {
        return (Mono) id2ResourceMap.get(id);
    }

    public static Flux unmountFlux(long id) {
        return (Flux) id2ResourceMap.get(id);
    }

}
