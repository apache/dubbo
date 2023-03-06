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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.RpcException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InjvmExporterListener extends ExporterListenerAdapter {

    private final Map<String, Set<ExporterChangeListener>> exporterChangeListeners = new ConcurrentHashMap<>();

    private final Map<String, Exporter<?>> exporters = new ConcurrentHashMap<>();

    @Override
    public void exported(Exporter<?> exporter) throws RpcException {
        String serviceKey = exporter.getInvoker().getUrl().getServiceKey();
        exporters.putIfAbsent(serviceKey, exporter);
        Set<ExporterChangeListener> listeners = exporterChangeListeners.get(serviceKey);
        if (!CollectionUtils.isEmpty(listeners)) {
            for (ExporterChangeListener listener : listeners) {
                listener.onExporterChangeExport(exporter);
            }
        }
        super.exported(exporter);
    }

    @Override
    public void unexported(Exporter<?> exporter) throws RpcException {
        String serviceKey = exporter.getInvoker().getUrl().getServiceKey();
        exporters.remove(serviceKey, exporter);
        Set<ExporterChangeListener> listeners = exporterChangeListeners.get(serviceKey);
        if (!CollectionUtils.isEmpty(listeners)) {
            for (ExporterChangeListener listener : listeners) {
                listener.onExporterChangeUnExport(exporter);
            }
        }

        super.unexported(exporter);
    }

    public synchronized void addExporterChangeListener(ExporterChangeListener listener, String serviceKey) {
        exporterChangeListeners.putIfAbsent(serviceKey, new ConcurrentHashSet<>());
        exporterChangeListeners.get(serviceKey).add(listener);
        if (exporters.get(serviceKey) != null) {
            Exporter<?> exporter = exporters.get(serviceKey);
            listener.onExporterChangeExport(exporter);
        }
    }

    public synchronized void removeExporterChangeListener(ExporterChangeListener listener, String listenerKey) {
        Set<ExporterChangeListener> listeners = exporterChangeListeners.get(listenerKey);
        listeners.remove(listener);
        if (CollectionUtils.isEmpty(listeners)) {
            exporterChangeListeners.remove(listenerKey);
        }
    }


}
