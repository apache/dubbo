package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.rpc.Exporter;

import java.util.Collection;

public interface DelegateExporterMap {

    /**
     * check is empty map
     * @return
     */
    boolean isEmpty();

    /**
     * get export
     * @param key
     * @return
     */
    Exporter<?> getExport(String key);

    /**
     * add
     * @param key
     * @param exporter
     */
    void addExportMap(String key, Exporter<?> exporter);

    /**
     * delete
     * @param key
     */
    void removeExportMap(String key, Exporter<?> exporter);

    /**
     * get all exports
     * @return
     */
    Collection<Exporter<?>> getExporters();
}
