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
package org.apache.dubbo.config.integration;

import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.filter.FilterChainBuilder;
import org.apache.dubbo.rpc.listener.ListenerExporterWrapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The abstraction of {@link ExporterListener} is to record exported exporters, which should be extended by different sub-classes.
 */
public abstract class AbstractRegistryCenterExporterListener implements ExporterListener {

    /**
     * Exported exporters.
     */
    private List<Exporter<?>> exportedExporters = new ArrayList();

    /**
     * Resolve all filters
     */
    private Set<Filter> filters = new HashSet<>();

    /**
     * Returns the interface of exported service.
     */
    protected abstract Class<?> getInterface();

    /**
     * {@inheritDoc}
     */
    @Override
    public void exported(Exporter<?> exporter) throws RpcException {
        ListenerExporterWrapper listenerExporterWrapper = (ListenerExporterWrapper) exporter;
        FilterChainBuilder.CallbackRegistrationInvoker callbackRegistrationInvoker = (FilterChainBuilder.CallbackRegistrationInvoker) listenerExporterWrapper.getInvoker();
        if (callbackRegistrationInvoker == null ||
            callbackRegistrationInvoker.getInterface() != getInterface()) {
            return;
        }
        exportedExporters.add(exporter);
        FilterChainBuilder.CopyOfFilterChainNode filterChainNode = getFilterChainNode(callbackRegistrationInvoker);
        do {
            Filter filter = this.getFilter(filterChainNode);
            if (filter != null) {
                filters.add(filter);
            }
            filterChainNode = this.getNextNode(filterChainNode);
        } while (filterChainNode != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unexported(Exporter<?> exporter) {
        exportedExporters.remove(exporter);
    }

    /**
     * Returns the exported exporters.
     */
    public List<Exporter<?>> getExportedExporters() {
        return Collections.unmodifiableList(exportedExporters);
    }

    /**
     * Returns all filters
     */
    public Set<Filter> getFilters() {
        return Collections.unmodifiableSet(filters);
    }

    /**
     * Use reflection to obtain {@link Filter}
     */
    private FilterChainBuilder.CopyOfFilterChainNode getFilterChainNode(FilterChainBuilder.CallbackRegistrationInvoker callbackRegistrationInvoker) {
        if (callbackRegistrationInvoker != null) {
            Field field = null;
            try {
                field = callbackRegistrationInvoker.getClass().getDeclaredField("filterInvoker");
                field.setAccessible(true);
                return (FilterChainBuilder.CopyOfFilterChainNode) field.get(callbackRegistrationInvoker);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Use reflection to obtain {@link Filter}
     */
    private Filter getFilter(FilterChainBuilder.CopyOfFilterChainNode filterChainNode) {
        if (filterChainNode != null) {
            Field field = null;
            try {
                field = filterChainNode.getClass().getDeclaredField("filter");
                field.setAccessible(true);
                return (Filter) field.get(filterChainNode);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Use reflection to obtain {@link FilterChainBuilder.CopyOfFilterChainNode}
     */
    private FilterChainBuilder.CopyOfFilterChainNode getNextNode(FilterChainBuilder.CopyOfFilterChainNode filterChainNode) {
        if (filterChainNode != null) {
            Field field = null;
            try {
                field = filterChainNode.getClass().getDeclaredField("nextNode");
                field.setAccessible(true);
                Object object = field.get(filterChainNode);
                if (object instanceof FilterChainBuilder.CopyOfFilterChainNode) {
                    return (FilterChainBuilder.CopyOfFilterChainNode) object;
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // ignore
            }
        }
        return null;
    }
}
