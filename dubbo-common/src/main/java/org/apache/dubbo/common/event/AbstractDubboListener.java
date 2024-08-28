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
package org.apache.dubbo.common.event;

import org.apache.dubbo.common.utils.ReflectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract Dubbo Listener. Match the event type by generic type.
 *
 * @param <E> the event to respond to
 */
public abstract class AbstractDubboListener<E extends DubboEvent> implements DubboListener<E> {

    private final Map<Class<?>, Boolean> eventMatchCache = new ConcurrentHashMap<>();

    /**
     * Whether to support the general determination of event points depends on the event type
     */
    @Override
    public boolean support(Class<? extends DubboEvent> eventClass) {
        return eventMatchCache.computeIfAbsent(eventClass, clazz -> {
            List<Class<?>> eventTypes = ReflectionUtils.getClassGenerics(getClass(), AbstractDubboListener.class);
            return eventTypes.stream().allMatch(eventType -> eventType.isAssignableFrom(clazz));
        });
    }
}
