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


/**
 * The InjvmExporterListener class is an implementation of the ExporterListenerAdapter abstract class,
 * <p>
 * which is used to listen for changes to the InjvmExporter instances.
 * <p>
 * It maintains two ConcurrentHashMaps, one to keep track of the ExporterChangeListeners registered for each service,
 * <p>
 * and another to keep track of the currently exported services and their associated Exporter instances.
 * <p>
 * It overrides the exported and unexported methods to add or remove the corresponding Exporter instances to/from
 * <p>
 * the exporters ConcurrentHashMap, and to notify all registered ExporterChangeListeners of the change.
 * <p>
 * It also provides methods to add or remove ExporterChangeListeners for a specific service, and to retrieve the
 * <p>
 * currently exported Exporter instance for a given service.
 */
public class InjvmExporterListener extends ExporterListenerAdapter {
    /*
     * A ConcurrentHashMap to keep track of the ExporterChangeListeners registered for each service.
     */
    private final Map<String, Set<ExporterChangeListener>> exporterChangeListeners = new ConcurrentHashMap<>();
    /*
     * A ConcurrentHashMap to keep track of the currently exported services and their associated Exporter instances
     */
    private final Map<String, Exporter<?>> exporters = new ConcurrentHashMap<>();

    /**
     * Overrides the exported method to add the given exporter to the exporters ConcurrentHashMap,
     * <p>
     * and to notify all registered ExporterChangeListeners of the export event.
     *
     * @param exporter The Exporter instance that has been exported.
     * @throws RpcException If there is an error during the export process.
     */
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


    /**
     * Overrides the unexported method to remove the given exporter from the exporters ConcurrentHashMap,
     * <p>
     * and to notify all registered ExporterChangeListeners of the unexport event.
     *
     * @param exporter The Exporter instance that has been unexported.
     * @throws RpcException If there is an error during the unexport process.
     */
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

    /**
     * Adds an ExporterChangeListener for a specific service, and notifies the listener of the current Exporter instance
     * <p>
     * if it exists.
     *
     * @param listener   The ExporterChangeListener to add.
     * @param serviceKey The service key for the service to listen for changes on.
     */
    public synchronized void addExporterChangeListener(ExporterChangeListener listener, String serviceKey) {
        exporterChangeListeners.putIfAbsent(serviceKey, new ConcurrentHashSet<>());
        exporterChangeListeners.get(serviceKey).add(listener);
        if (exporters.get(serviceKey) != null) {
            Exporter<?> exporter = exporters.get(serviceKey);
            listener.onExporterChangeExport(exporter);
        }
    }

    /**
     * Removes an ExporterChangeListener for a specific service.
     *
     * @param listener    The ExporterChangeListener to remove.
     * @param listenerKey The service key for the service to remove the listener from.
     */
    public synchronized void removeExporterChangeListener(ExporterChangeListener listener, String listenerKey) {
        Set<ExporterChangeListener> listeners = exporterChangeListeners.get(listenerKey);
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        listeners.remove(listener);
        if (CollectionUtils.isEmpty(listeners)) {
            exporterChangeListeners.remove(listenerKey);
        }
    }


}
