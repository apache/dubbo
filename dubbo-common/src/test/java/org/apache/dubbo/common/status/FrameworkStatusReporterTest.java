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
package org.apache.dubbo.common.status;

import com.google.gson.Gson;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReporter;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INSTANCE;
import static org.apache.dubbo.common.status.reporter.FrameworkStatusReporter.ADDRESS_CONSUMPTION_STATUS;
import static org.apache.dubbo.common.status.reporter.FrameworkStatusReporter.MIGRATION_STEP_STATUS;
import static org.apache.dubbo.common.status.reporter.FrameworkStatusReporter.REGISTRATION_STATUS;
import static org.apache.dubbo.common.status.reporter.FrameworkStatusReporter.createConsumptionReport;
import static org.apache.dubbo.common.status.reporter.FrameworkStatusReporter.createRegistrationReport;

public class FrameworkStatusReporterTest {

    @BeforeAll
    public static void init() {
        ApplicationModel.getConfigManager().setApplication(new ApplicationConfig("Test"));
    }

    @AfterAll
    public static void destroy() {
        ApplicationModel.getConfigManager().setApplication(null);
        ApplicationModel.reset();
    }

    @Test
    public void test() {
        FrameworkStatusReporter.reportRegistrationStatus(createRegistrationReport(DEFAULT_REGISTER_MODE_INSTANCE));

        URL consumerURL = Mockito.mock(URL.class);
        Mockito.when(consumerURL.getServiceInterface()).thenReturn("Test");
        Mockito.when(consumerURL.getGroup()).thenReturn("Group");
        Mockito.when(consumerURL.getVersion()).thenReturn("0.0.0");
        Mockito.when(consumerURL.getServiceKey()).thenReturn("Group/Test:0.0.0");
        Mockito.when(consumerURL.getDisplayServiceKey()).thenReturn("Test:0.0.0");
        FrameworkStatusReporter.reportConsumptionStatus(
            createConsumptionReport(consumerURL.getServiceInterface(), consumerURL.getVersion(), consumerURL.getGroup(), "app")
        );
        FrameworkStatusReporter.reportRegistrationStatus(createRegistrationReport(DEFAULT_REGISTER_MODE_INSTANCE));


        FrameworkStatusReporter.reportMigrationStepStatus(
            FrameworkStatusReporter.createMigrationStepReport(consumerURL.getServiceInterface(), consumerURL.getVersion(),
                consumerURL.getGroup(), "FORCE_INTERFACE", "FORCE_APPLICATION", "ture"));

        ExtensionLoader<FrameworkStatusReporter> loader = ExtensionLoader.getExtensionLoader(FrameworkStatusReporter.class);
        MockFrameworkStatusReporter statusReporter = (MockFrameworkStatusReporter) loader.getLoadedExtension("mock");
        //"migrationStepStatus" -> "{"originStep":"FORCE_INTERFACE","application":"Test","service":"Test","success":"ture","newStep":"FORCE_APPLICATION","type":"migrationStepStatus","version":"0.0.0","group":"Group"}"
        //"registration" -> "{"application":"Test","status":"instance"}"
        //"consumption" -> "{"application":"Test","service":"Test","type":"consumption","version":"0.0.0","group":"Group","status":"app"}"
        Map<String, Object> reportContent = statusReporter.getReportContent();
        Assertions.assertEquals(reportContent.size(), 3);

        Gson gson = new Gson();
        Object registrationStatus = reportContent.get(REGISTRATION_STATUS);
        Map<String, String> registrationMap = gson.fromJson(String.valueOf(registrationStatus), Map.class);
        Assertions.assertEquals(registrationMap.get("application"), "Test");
        Assertions.assertEquals(registrationMap.get("status"), "instance");

        Object addressConsumptionStatus = reportContent.get(ADDRESS_CONSUMPTION_STATUS);
        Map<String, String> consumptionMap = gson.fromJson(String.valueOf(addressConsumptionStatus), Map.class);
        Assertions.assertEquals(consumptionMap.get("application"), "Test");
        Assertions.assertEquals(consumptionMap.get("service"), "Test");
        Assertions.assertEquals(consumptionMap.get("status"), "app");
        Assertions.assertEquals(consumptionMap.get("type"), "consumption");
        Assertions.assertEquals(consumptionMap.get("version"), "0.0.0");
        Assertions.assertEquals(consumptionMap.get("group"), "Group");

        Object migrationStepStatus = reportContent.get(MIGRATION_STEP_STATUS);
        Map<String, String> migrationStepStatusMap = gson.fromJson(String.valueOf(migrationStepStatus), Map.class);
        Assertions.assertEquals(migrationStepStatusMap.get("originStep"), "FORCE_INTERFACE");
        Assertions.assertEquals(migrationStepStatusMap.get("application"), "Test");
        Assertions.assertEquals(migrationStepStatusMap.get("service"), "Test");
        Assertions.assertEquals(migrationStepStatusMap.get("success"), "ture");
        Assertions.assertEquals(migrationStepStatusMap.get("newStep"), "FORCE_APPLICATION");
        Assertions.assertEquals(migrationStepStatusMap.get("type"), "migrationStepStatus");
        Assertions.assertEquals(migrationStepStatusMap.get("version"), "0.0.0");
        Assertions.assertEquals(migrationStepStatusMap.get("group"), "Group");


    }
}
