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

import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;

import java.util.concurrent.atomic.AtomicInteger;

public class ReferenceCountExporter<T> implements Exporter<T> {
    private final Exporter<T> exporter;
    private final String providerKey;
    private final ExporterFactory exporterFactory;
    private final AtomicInteger count = new AtomicInteger(0);

    public ReferenceCountExporter(Exporter<T> exporter, String providerKey, ExporterFactory exporterFactory) {
        this.exporter = exporter;
        this.providerKey = providerKey;
        this.exporterFactory = exporterFactory;
    }

    @Override
    public Invoker<T> getInvoker() {
        return exporter.getInvoker();
    }

    public void increaseCount() {
        count.incrementAndGet();
    }

    @Override
    public void unexport() {
        if (count.decrementAndGet() == 0) {
            exporter.unexport();
        }
        exporterFactory.remove(providerKey, this);
    }

    @Override
    public void register() {}

    @Override
    public void unregister() {}
}
