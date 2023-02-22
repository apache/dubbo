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
package org.apache.dubbo.rpc.listener;

import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.RpcException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InjvmExporterListener extends ExporterListenerAdapter {

    private final List<ExporterChangeListener> exporterChangeListeners = new CopyOnWriteArrayList<>();

    private final Map<String, Exporter<?>> exporters = new ConcurrentHashMap<>();

    @Override
    public void exported(Exporter<?> exporter) throws RpcException {
        exporters.putIfAbsent(exporter.getInvoker().getUrl().getServiceKey(), exporter);
        for (ExporterChangeListener listener : exporterChangeListeners) {
            listener.onExporterChangeExport(exporters);
        }
        super.exported(exporter);
    }

    @Override
    public void unexported(Exporter<?> exporter) throws RpcException {
        exporters.remove(exporter.getInvoker().getUrl().getServiceKey(), exporter);
        super.unexported(exporter);
    }

    public synchronized void addExporterChangeListener(ExporterChangeListener listener, String serviceKey) {
        exporterChangeListeners.add(listener);
        if (exporters.get(serviceKey) != null) {
            listener.onExporterChangeExport(exporters);
        }
    }


}
