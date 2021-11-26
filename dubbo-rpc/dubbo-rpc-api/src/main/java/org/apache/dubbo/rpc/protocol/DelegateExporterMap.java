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
package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Exporter;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * delegate exportermap oper
 */
public class DelegateExporterMap {
    protected final Map<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>();

    /**
     * check is empty map
     * @return
     */
    public boolean isEmpty() {
        return CollectionUtils.isEmptyMap(exporterMap);
    }

    /**
     * get export
     * @param key
     * @return
     */
    public Exporter<?> getExport(String key) {
        return exporterMap.get(key);
    }

    /**
     * add
     * @param key
     * @param exporter
     */
    public void addExportMap(String key, Exporter<?> exporter) {
        exporterMap.put(key, exporter);
    }

    /**
     * delete
     * @param key
     */
    public boolean removeExportMap(String key, Exporter<?> exporter) {
        Exporter<?> findExporter = exporterMap.get(key);
        if(findExporter == exporter){
            exporterMap.remove(key);
            return true;
        }
        return false;
    }

    /**
     * get the exports
     * @return
     */
    public Map<String, Exporter<?>> getExporterMap() {
        return exporterMap;
    }

    /**
     * get all exports
     * @return
     */
    public Collection<Exporter<?>> getExporters() {
        return Collections.unmodifiableCollection(exporterMap.values());
    }
}
