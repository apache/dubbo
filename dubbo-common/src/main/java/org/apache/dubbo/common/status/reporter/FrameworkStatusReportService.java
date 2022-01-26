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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Set;

public class FrameworkStatusReportService implements ScopeModelAware {

    private static final Logger logger = LoggerFactory.getLogger(FrameworkStatusReporter.class);
    public static final String REGISTRATION_STATUS = "registration";
    public static final String ADDRESS_CONSUMPTION_STATUS = "consumption";
    public static final String MIGRATION_STEP_STATUS = "migrationStepStatus";

    private ApplicationModel applicationModel;
    private Set<FrameworkStatusReporter> reporters;
    private Gson gson = new Gson();

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        reporters = applicationModel.getExtensionLoader(FrameworkStatusReporter.class).getSupportedExtensionInstances();
    }

    public void reportRegistrationStatus(Object obj) {
        doReport(REGISTRATION_STATUS, obj);
    }

    public void reportConsumptionStatus(Object obj) {
        doReport(ADDRESS_CONSUMPTION_STATUS, obj);
    }

    public void reportMigrationStepStatus(Object obj) {
        doReport(MIGRATION_STEP_STATUS, obj);
    }

    public boolean hasReporter() {
        return reporters.size() > 0;
    }

    private void doReport(String type, Object obj) {
        // TODO, report asynchronously
        try {
            if (CollectionUtils.isNotEmpty(reporters)) {
                for (FrameworkStatusReporter reporter : reporters) {
                    reporter.report(type, obj);
                }
            }
        } catch (Exception e) {
            logger.info("Report " + type + " status failed because of " + e.getMessage());
        }
    }

    public String createRegistrationReport(String status) {
        HashMap<String, String> registration = new HashMap<>();
        registration.put("application", applicationModel.getApplicationName());
        registration.put("status", status);
        return gson.toJson(registration);
    }

    public String createConsumptionReport(String interfaceName, String version, String group, String status) {
        HashMap<String, String> migrationStatus = new HashMap<>();
        migrationStatus.put("type", "consumption");
        migrationStatus.put("application", applicationModel.getApplicationName());
        migrationStatus.put("service", interfaceName);
        migrationStatus.put("version", version);
        migrationStatus.put("group", group);
        migrationStatus.put("status", status);
        return gson.toJson(migrationStatus);
    }

    public String createMigrationStepReport(String interfaceName, String version, String group, String originStep, String newStep, String success) {
        HashMap<String, String> migrationStatus = new HashMap<>();
        migrationStatus.put("type", "migrationStepStatus");
        migrationStatus.put("application", applicationModel.getApplicationName());
        migrationStatus.put("service", interfaceName);
        migrationStatus.put("version", version);
        migrationStatus.put("group", group);
        migrationStatus.put("originStep", originStep);
        migrationStatus.put("newStep", newStep);
        migrationStatus.put("success", success);
        return gson.toJson(migrationStatus);
    }
}
