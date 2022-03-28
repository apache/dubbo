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
package org.apache.dubbo.metadata.report.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.report.MetadataReport;
import org.apache.dubbo.metadata.report.MetadataReportFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;

public abstract class AbstractMetadataReportFactory implements MetadataReportFactory {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMetadataReportFactory.class);
    private static final String EXPORT_KEY = "export";
    private static final String REFER_KEY = "refer";

    /**
     * The lock for the acquisition process of the registry
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Registry Collection Map<metadataAddress, MetadataReport>
     */
    private final Map<String, MetadataReport> serviceStoreMap = new ConcurrentHashMap<>();

    @Override
    public MetadataReport getMetadataReport(URL url) {
        url = url.setPath(MetadataReport.class.getName())
            .removeParameters(EXPORT_KEY, REFER_KEY);
        String key = url.toServiceString();

        MetadataReport metadataReport = serviceStoreMap.get(key);
        if (metadataReport != null) {
            return metadataReport;
        }

        // Lock the metadata access process to ensure a single instance of the metadata instance
        lock.lock();
        try {
            metadataReport = serviceStoreMap.get(key);
            if (metadataReport != null) {
                return metadataReport;
            }
            boolean check = url.getParameter(CHECK_KEY, true) && url.getPort() != 0;
            try {
                metadataReport = createMetadataReport(url);
            } catch (Exception e) {
                if (!check) {
                    logger.warn("The metadata reporter failed to initialize", e);
                } else {
                    throw e;
                }
            }

            if (check && metadataReport == null) {
                throw new IllegalStateException("Can not create metadata Report " + url);
            }
            if (metadataReport != null) {
                serviceStoreMap.put(key, metadataReport);
            }
            return metadataReport;
        } finally {
            // Release the lock
            lock.unlock();
        }
    }

    @Override
    public void destroy() {
        lock.lock();
        try {
            for (MetadataReport metadataReport : serviceStoreMap.values()) {
                try{
                    metadataReport.destroy();
                }catch (Throwable ignored){
                    // ignored
                    logger.warn(ignored.getMessage(),ignored);
                }
            }
            serviceStoreMap.clear();
        } finally {
            lock.unlock();
        }
    }

    protected abstract MetadataReport createMetadataReport(URL url);
}
