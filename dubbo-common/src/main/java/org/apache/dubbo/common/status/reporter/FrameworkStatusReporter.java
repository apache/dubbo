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
package org.apache.dubbo.common.status.reporter;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Set;

@SPI
public interface FrameworkStatusReporter {
    static final Gson gson = new Gson();
    Logger logger = LoggerFactory.getLogger(FrameworkStatusReporter.class);
    String REGISTRATION_STATUS = "registration";
    String ADDRESS_CONSUMPTION_STATUS = "consumption";
    String MIGRATION_STEP_STATUS = "migrationStepStatus";

    void report(String type, Object obj);

    static void reportRegistrationStatus(Object obj) {
        doReport(REGISTRATION_STATUS, obj);
    }

    static void reportConsumptionStatus(Object obj) {
        doReport(ADDRESS_CONSUMPTION_STATUS, obj);
    }

    static void reportMigrationStepStatus(Object obj) {
        doReport(MIGRATION_STEP_STATUS, obj);
    }

    static boolean hasReporter() {
        return ExtensionLoader.getExtensionLoader(FrameworkStatusReporter.class).getSupportedExtensions().size() > 0;
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

    static String createRegistrationReport(String status) {
        return "{\"application\":\"" +
                ApplicationModel.getName() +
                "\",\"status\":\"" +
                status +
                "\"}";
    }

    static String createConsumptionReport(String interfaceName, String version, String group, String status) {
        HashMap<String, String> migrationStatus = new HashMap<>();
        migrationStatus.put("type", "consumption");
        migrationStatus.put("application", ApplicationModel.getName());
        migrationStatus.put("service", interfaceName);
        migrationStatus.put("version", version);
        migrationStatus.put("group", group);
        migrationStatus.put("status", status);
        return gson.toJson(migrationStatus);
    }

    static String createMigrationStepReport(String interfaceName, String version, String group, String originStep, String newStep, String success) {
        HashMap<String, String> migrationStatus = new HashMap<>();
        migrationStatus.put("type", "migrationStepStatus");
        migrationStatus.put("application", ApplicationModel.getName());
        migrationStatus.put("service", interfaceName);
        migrationStatus.put("version", version);
        migrationStatus.put("group", group);
        migrationStatus.put("originStep", originStep);
        migrationStatus.put("newStep", newStep);
        migrationStatus.put("success", success);
        return gson.toJson(migrationStatus);
    }
}
