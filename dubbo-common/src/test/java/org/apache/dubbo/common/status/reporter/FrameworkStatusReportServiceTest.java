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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_REGISTER_MODE_INSTANCE;
import static org.apache.dubbo.common.status.reporter.FrameworkStatusReportService.ADDRESS_CONSUMPTION_STATUS;
import static org.apache.dubbo.common.status.reporter.FrameworkStatusReportService.MIGRATION_STEP_STATUS;
import static org.apache.dubbo.common.status.reporter.FrameworkStatusReportService.REGISTRATION_STATUS;

/**
 * {@link FrameworkStatusReportService}
 */
class FrameworkStatusReportServiceTest {

    @Test
    void test() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ApplicationConfig app = new ApplicationConfig("APP");
        applicationModel.getApplicationConfigManager().setApplication(app);
        FrameworkStatusReportService reportService =
                applicationModel.getBeanFactory().getBean(FrameworkStatusReportService.class);

        // 1. reportRegistrationStatus
        reportService.reportRegistrationStatus(reportService.createRegistrationReport(DEFAULT_REGISTER_MODE_INSTANCE));

        // 2. createConsumptionReport
        URL consumerURL = Mockito.mock(URL.class);
        Mockito.when(consumerURL.getServiceInterface()).thenReturn("Test");
        Mockito.when(consumerURL.getGroup()).thenReturn("Group");
        Mockito.when(consumerURL.getVersion()).thenReturn("0.0.0");
        Mockito.when(consumerURL.getServiceKey()).thenReturn("Group/Test:0.0.0");
        Mockito.when(consumerURL.getDisplayServiceKey()).thenReturn("Test:0.0.0");
        reportService.reportConsumptionStatus(reportService.createConsumptionReport(
                consumerURL.getServiceInterface(), consumerURL.getVersion(), consumerURL.getGroup(), "status"));

        // 3. reportMigrationStepStatus
        reportService.reportMigrationStepStatus(reportService.createMigrationStepReport(
                consumerURL.getServiceInterface(),
                consumerURL.getVersion(),
                consumerURL.getGroup(),
                "FORCE_INTERFACE",
                "FORCE_APPLICATION",
                "true"));

        MockFrameworkStatusReporter statusReporter =
                (MockFrameworkStatusReporter) applicationModel.getExtension(FrameworkStatusReporter.class, "mock");

        // "migrationStepStatus" ->
        // "{"originStep":"FORCE_INTERFACE","application":"APP","service":"Test","success":"true","newStep":"FORCE_APPLICATION","type":"migrationStepStatus","version":"0.0.0","group":"Group"}"
        // "registration" -> "{"application":"APP","status":"instance"}"
        // "consumption" ->
        // "{"application":"APP","service":"Test","type":"consumption","version":"0.0.0","group":"Group","status":"status"}"
        Map<String, Object> reportContent = statusReporter.getReportContent();
        Assertions.assertEquals(reportContent.size(), 3);

        // verify registrationStatus
        Object registrationStatus = reportContent.get(REGISTRATION_STATUS);
        Map<String, String> registrationMap = JsonUtils.toJavaObject(String.valueOf(registrationStatus), Map.class);
        Assertions.assertEquals(registrationMap.get("application"), "APP");
        Assertions.assertEquals(registrationMap.get("status"), "instance");

        // verify addressConsumptionStatus
        Object addressConsumptionStatus = reportContent.get(ADDRESS_CONSUMPTION_STATUS);
        Map<String, String> consumptionMap =
                JsonUtils.toJavaObject(String.valueOf(addressConsumptionStatus), Map.class);
        Assertions.assertEquals(consumptionMap.get("application"), "APP");
        Assertions.assertEquals(consumptionMap.get("service"), "Test");
        Assertions.assertEquals(consumptionMap.get("status"), "status");
        Assertions.assertEquals(consumptionMap.get("type"), "consumption");
        Assertions.assertEquals(consumptionMap.get("version"), "0.0.0");
        Assertions.assertEquals(consumptionMap.get("group"), "Group");

        // verify migrationStepStatus
        Object migrationStepStatus = reportContent.get(MIGRATION_STEP_STATUS);
        Map<String, String> migrationStepStatusMap =
                JsonUtils.toJavaObject(String.valueOf(migrationStepStatus), Map.class);
        Assertions.assertEquals(migrationStepStatusMap.get("originStep"), "FORCE_INTERFACE");
        Assertions.assertEquals(migrationStepStatusMap.get("application"), "APP");
        Assertions.assertEquals(migrationStepStatusMap.get("service"), "Test");
        Assertions.assertEquals(migrationStepStatusMap.get("success"), "true");
        Assertions.assertEquals(migrationStepStatusMap.get("newStep"), "FORCE_APPLICATION");
        Assertions.assertEquals(migrationStepStatusMap.get("type"), "migrationStepStatus");
        Assertions.assertEquals(migrationStepStatusMap.get("version"), "0.0.0");
        Assertions.assertEquals(migrationStepStatusMap.get("group"), "Group");

        frameworkModel.destroy();
    }

    @Test
    void testReportRegistrationOutcomeSuccess() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ApplicationConfig app = new ApplicationConfig("APP");
        applicationModel.getApplicationConfigManager().setApplication(app);
        FrameworkStatusReportService reportService =
                applicationModel.getBeanFactory().getBean(FrameworkStatusReportService.class);

        reportService.reportRegistrationOutcome(
                "INTERFACE_REGISTER", "127.0.0.1:2181", "GroupA/DemoService:1.0.0", true, null);

        MockFrameworkStatusReporter statusReporter =
                (MockFrameworkStatusReporter) applicationModel.getExtension(FrameworkStatusReporter.class, "mock");
        Object registrationStatus = statusReporter.getReportContent().get(REGISTRATION_STATUS);
        Assertions.assertNotNull(registrationStatus, "registration outcome should be reported");
        Map<String, String> payload = JsonUtils.toJavaObject(String.valueOf(registrationStatus), Map.class);
        Assertions.assertEquals("APP", payload.get("application"));
        Assertions.assertEquals("INTERFACE_REGISTER", payload.get("mode"));
        Assertions.assertEquals("127.0.0.1:2181", payload.get("registry"));
        Assertions.assertEquals("GroupA/DemoService:1.0.0", payload.get("service"));
        Assertions.assertEquals("SUCCESS", payload.get("status"));
        Assertions.assertNull(payload.get("error"), "success outcome must not carry an error field");

        frameworkModel.destroy();
    }

    @Test
    void testReportRegistrationOutcomeFailure() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ApplicationConfig app = new ApplicationConfig("APP");
        applicationModel.getApplicationConfigManager().setApplication(app);
        FrameworkStatusReportService reportService =
                applicationModel.getBeanFactory().getBean(FrameworkStatusReportService.class);

        reportService.reportRegistrationOutcome(
                "INSTANCE_REGISTER", "127.0.0.1:2181", "GroupA/DemoService:1.0.0", false, "NoNode for /dubbo/...");

        MockFrameworkStatusReporter statusReporter =
                (MockFrameworkStatusReporter) applicationModel.getExtension(FrameworkStatusReporter.class, "mock");
        Object registrationStatus = statusReporter.getReportContent().get(REGISTRATION_STATUS);
        Map<String, String> payload = JsonUtils.toJavaObject(String.valueOf(registrationStatus), Map.class);
        Assertions.assertEquals("INSTANCE_REGISTER", payload.get("mode"));
        Assertions.assertEquals("FAILED", payload.get("status"));
        Assertions.assertEquals("NoNode for /dubbo/...", payload.get("error"));

        frameworkModel.destroy();
    }

    @Test
    void testReportRegistrationOutcomePendingRetry() {
        FrameworkModel frameworkModel = new FrameworkModel();
        ApplicationModel applicationModel = frameworkModel.newApplication();
        ApplicationConfig app = new ApplicationConfig("APP");
        applicationModel.getApplicationConfigManager().setApplication(app);
        FrameworkStatusReportService reportService =
                applicationModel.getBeanFactory().getBean(FrameworkStatusReportService.class);

        reportService.reportRegistrationOutcome(
                "INTERFACE_REGISTER",
                "127.0.0.1:2181",
                "GroupA/DemoService:1.0.0",
                FrameworkStatusReportService.OUTCOME_PENDING_RETRY,
                "initial registration attempt failed; waiting for retry");

        MockFrameworkStatusReporter statusReporter =
                (MockFrameworkStatusReporter) applicationModel.getExtension(FrameworkStatusReporter.class, "mock");
        Map<String, String> payload = JsonUtils.toJavaObject(
                String.valueOf(statusReporter.getReportContent().get(REGISTRATION_STATUS)), Map.class);
        Assertions.assertEquals("INTERFACE_REGISTER", payload.get("mode"));
        Assertions.assertEquals("PENDING_RETRY", payload.get("status"));
        Assertions.assertEquals("initial registration attempt failed; waiting for retry", payload.get("error"));

        frameworkModel.destroy();
    }
}
