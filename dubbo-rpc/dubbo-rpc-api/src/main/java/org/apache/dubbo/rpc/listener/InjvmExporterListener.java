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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InjvmExporterListener extends ExporterListenerAdapter {

    private final Map<String, ExporterChangeListener> exporterChangeListeners = new ConcurrentHashMap<>();

    private final Map<String, Exporter<?>> exporters = new ConcurrentHashMap<>();

    @Override
    public void exported(Exporter<?> exporter) throws RpcException {
        String serviceKey = exporter.getInvoker().getUrl().getServiceKey();
        exporters.putIfAbsent(exporter.getInvoker().getUrl().getServiceKey(), exporter);
        ExporterChangeListener exporterChangeListener = exporterChangeListeners.get(serviceKey);
        if (exporterChangeListener != null) {
            exporterChangeListener.onExporterChangeExport(exporter);
        }
        super.exported(exporter);
    }

    @Override
    public void unexported(Exporter<?> exporter) throws RpcException {
        String serviceKey = exporter.getInvoker().getUrl().getServiceKey();
        exporters.remove(serviceKey, exporter);
        ExporterChangeListener exporterChangeListener = exporterChangeListeners.remove(serviceKey);
        if (exporterChangeListener != null) {
            exporterChangeListener.onExporterChangeUnExport(exporter);
        }
        super.unexported(exporter);
    }

    public synchronized void addExporterChangeListener(ExporterChangeListener listener, String serviceKey) {
        exporterChangeListeners.putIfAbsent(serviceKey, listener);
        if (exporters.get(serviceKey) != null) {
            Exporter<?> exporter = exporters.get(serviceKey);
            listener.onExporterChangeExport(exporter);
        }
    }

    public synchronized void removeExporterChangeListener(String listenerKey) {
        exporterChangeListeners.remove(listenerKey);
    }


}
