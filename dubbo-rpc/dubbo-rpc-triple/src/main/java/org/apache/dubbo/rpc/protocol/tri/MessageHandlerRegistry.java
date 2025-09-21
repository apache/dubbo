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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.TypeUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageHandlerRegistry {

    private final Map<Class<?>, Map<String, InputMessageHandler<?>>> inputHandlers = new ConcurrentHashMap<>();
    private final Map<Class<?>, OutputMessageHandler<?>> outputHandlers = new ConcurrentHashMap<>();

    public MessageHandlerRegistry(FrameworkModel frameworkModel) {
        frameworkModel
                .getExtensionLoader(InputMessageHandler.class)
                .getSupportedExtensionInstances()
                .forEach(this::register);
        frameworkModel
                .getExtensionLoader(OutputMessageHandler.class)
                .getSupportedExtensionInstances()
                .forEach(this::register);
    }

    public <INPUT> void register(InputMessageHandler<INPUT> handler) {
        Class<?> inputType = TypeUtils.findActualTypeArgument(handler.getClass(), InputMessageHandler.class, 0);
        inputHandlers
                .computeIfAbsent(inputType, clazz -> new ConcurrentHashMap<>())
                .put(handler.supportProtocol(), handler);
    }

    public <OUTPUT> void register(OutputMessageHandler<OUTPUT> handler) {
        Class<?> outputType = TypeUtils.findActualTypeArgument(handler.getClass(), OutputMessageHandler.class, 0);
        outputHandlers.put(outputType, handler);
    }

    @SuppressWarnings("unchecked")
    public <INPUT> InputMessageHandler<INPUT> get(Class<INPUT> inputType, String protocol) {
        Map<String, InputMessageHandler<?>> inputMessageHandlerMap = inputHandlers.get(inputType);
        if (CollectionUtils.isEmptyMap(inputMessageHandlerMap)) {
            return null;
        }
        return (InputMessageHandler<INPUT>) inputMessageHandlerMap.get(protocol);
    }

    @SuppressWarnings("unchecked")
    public <OUTPUT> OutputMessageHandler<OUTPUT> get(Class<OUTPUT> outputType) {
        return (OutputMessageHandler<OUTPUT>) outputHandlers.get(outputType);
    }
}
