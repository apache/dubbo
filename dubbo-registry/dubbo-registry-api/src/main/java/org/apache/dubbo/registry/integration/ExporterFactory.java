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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExporterFactory {
    private final Map<String, ReferenceCountExporter<?>> exporters = new ConcurrentHashMap<>();

    private final Protocol protocol;

    public ExporterFactory(FrameworkModel frameworkModel) {
        this.protocol = frameworkModel.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    }

    protected ReferenceCountExporter<?> createExporter(String providerKey, Invoker<?> invoker) {
        return exporters.computeIfAbsent(providerKey,
            key -> new ReferenceCountExporter<>(protocol.export(invoker), key, this));
    }

    protected void remove(String key, ReferenceCountExporter<?> exporter) {
        exporters.remove(key, exporter);
    }
}
