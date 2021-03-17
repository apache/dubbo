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
package org.apache.dubbo.common.reporter;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.Set;

@SPI
public interface FrameworkStatusReporter {
    Logger logger = LoggerFactory.getLogger(FrameworkStatusReporter.class);
    String REGISTRATION_STATUS = "registration";
    String ADDRESS_CONSUMPTION_STATUS = "consumption";

    void report(String type, Object obj);

    static void reportRegistrationStatus(Object obj) {
        doReport(REGISTRATION_STATUS, obj);
    }

    static void reportConsumptionStatus(Object obj) {
        doReport(ADDRESS_CONSUMPTION_STATUS, obj);
    }

    static void doReport(String type, Object obj) {
        // TODO, report asynchronously
        try {
            Set<FrameworkStatusReporter> reporters = ExtensionLoader.getExtensionLoader(FrameworkStatusReporter.class).getSupportedExtensionInstances();
            if (CollectionUtils.isNotEmpty(reporters)) {
                FrameworkStatusReporter reporter = reporters.iterator().next();
                reporter.report(type, obj);
            }
        } catch (Exception e) {
            logger.info("Report " + type + " status failed because of " + e.getMessage());
        }
    }
}
