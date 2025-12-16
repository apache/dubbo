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
package org.apache.dubbo.common.nacos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.nacos.NacosAppNameUtils.NACOS_SET_PROJECT_NAME_KEY;
import static org.apache.dubbo.common.nacos.NacosAppNameUtils.PROJECT_NAME_SYS_PROP_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link NacosAppNameUtils}
 */
class NacosAppNameUtilsTest {

    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private String originalProjectName;

    @BeforeEach
    void setUp() {
        // Save original system property
        originalProjectName = System.getProperty(PROJECT_NAME_SYS_PROP_KEY);
        // Clear the system property before each test
        System.clearProperty(PROJECT_NAME_SYS_PROP_KEY);

        frameworkModel = new FrameworkModel();
        applicationModel = frameworkModel.newApplication();
    }

    @AfterEach
    void tearDown() {
        // Restore original system property
        if (originalProjectName != null) {
            System.setProperty(PROJECT_NAME_SYS_PROP_KEY, originalProjectName);
        } else {
            System.clearProperty(PROJECT_NAME_SYS_PROP_KEY);
        }

        if (frameworkModel != null) {
            frameworkModel.destroy();
        }
    }

    @Test
    void testNullUrl() {
        // When URL is null, method should return early without setting any property
        NacosAppNameUtils.maybeSetProjectName(null, applicationModel, null);
        assertNull(System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }

    @Test
    void testFeatureDisabledByDefault() {
        // When nacos.set-project-name is not set (defaults to false), property should not be set
        URL url = URL.valueOf("nacos://127.0.0.1:8848");

        NacosAppNameUtils.maybeSetProjectName(url, applicationModel, null);

        assertNull(System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }

    @Test
    void testFeatureExplicitlyDisabled() {
        // When nacos.set-project-name=false, property should not be set
        URL url = URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=false");

        NacosAppNameUtils.maybeSetProjectName(url, applicationModel, null);

        assertNull(System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }

    @Test
    void testSystemPropertyAlreadySet() {
        // When project.name system property is already set, it should not be overwritten
        String existingName = "existing-app-name";
        System.setProperty(PROJECT_NAME_SYS_PROP_KEY, existingName);

        ApplicationConfig appConfig = new ApplicationConfig("my-dubbo-app");
        applicationModel.getApplicationConfigManager().setApplication(appConfig);

        URL url = URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=true");

        NacosAppNameUtils.maybeSetProjectName(url, applicationModel, null);

        // Should remain the existing value
        assertEquals(existingName, System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }

    @Test
    void testApplicationModelPriority() {
        // ApplicationModel should have highest priority
        String appName = "my-dubbo-app";
        ApplicationConfig appConfig = new ApplicationConfig(appName);
        applicationModel.getApplicationConfigManager().setApplication(appConfig);

        URL url =
                URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=true&application=url-app-name");

        NacosAppNameUtils.maybeSetProjectName(url, applicationModel, null);

        assertEquals(appName, System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }

    @Test
    void testUrlApplicationParameterFallback() {
        // When ApplicationModel has no app name, URL's application parameter should be used
        URL url =
                URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=true&application=url-app-name");

        // Pass null ApplicationModel to skip priority 1
        NacosAppNameUtils.maybeSetProjectName(url, null, null);

        assertEquals("url-app-name", System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }

    @Test
    void testScopeModelFallback() {
        // When ApplicationModel is null and URL has no application param,
        // ScopeModel from URL should be used
        String appName = "scope-model-app";
        ApplicationConfig appConfig = new ApplicationConfig(appName);
        applicationModel.getApplicationConfigManager().setApplication(appConfig);

        URL url = URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=true");
        url = url.setScopeModel(applicationModel);

        // Pass null ApplicationModel to force ScopeModel fallback
        NacosAppNameUtils.maybeSetProjectName(url, null, null);

        assertEquals(appName, System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }

    @Test
    void testApplicationModelWithoutConfigThrowsException() {
        // When ApplicationModel has no ApplicationConfig set, getApplicationName() throws
        // IllegalStateException. The exception is caught in maybeSetProjectName() and logged,
        // but the property will not be set (the method returns early after catching the exception).
        URL url =
                URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=true&application=fallback-app");

        // Create a new application model without setting ApplicationConfig
        FrameworkModel fm = new FrameworkModel();
        ApplicationModel emptyAppModel = fm.newApplication();

        try {
            NacosAppNameUtils.maybeSetProjectName(url, emptyAppModel, null);

            // Since ApplicationModel.getApplicationName() throws an exception when no
            // ApplicationConfig is set, the exception is caught and the property is not set.
            // The URL's application parameter is NOT used as fallback because the exception
            // short-circuits the entire method.
            assertNull(System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
        } finally {
            fm.destroy();
        }
    }

    @Test
    void testAllSourcesEmpty() {
        // When all sources return empty/null app name, property should not be set
        URL url = URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=true");

        // Pass null ApplicationModel, URL has no application param, and URL has no ScopeModel set.
        // In getApplicationName():
        //   - Priority 1: applicationModel is null, skipped
        //   - Priority 2: url.getApplication() returns null
        //   - Priority 3: ScopeModelUtil.getOrNullApplicationModel(null) returns null
        // So getApplicationName() returns null, and property is not set.
        NacosAppNameUtils.maybeSetProjectName(url, null, null);

        assertNull(System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }

    @Test
    void testApplicationModelTakesPriorityOverScopeModel() {
        // When both ApplicationModel parameter and URL ScopeModel are available,
        // ApplicationModel parameter should take priority
        String appModelName = "app-model-name";
        String scopeModelName = "scope-model-name";

        // Set up ApplicationModel with one name
        ApplicationConfig appConfig1 = new ApplicationConfig(appModelName);
        applicationModel.getApplicationConfigManager().setApplication(appConfig1);

        // Set up another ApplicationModel for ScopeModel with different name
        FrameworkModel fm2 = new FrameworkModel();
        ApplicationModel scopeAppModel = fm2.newApplication();
        ApplicationConfig appConfig2 = new ApplicationConfig(scopeModelName);
        scopeAppModel.getApplicationConfigManager().setApplication(appConfig2);

        try {
            URL url = URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=true");
            url = url.setScopeModel(scopeAppModel);

            NacosAppNameUtils.maybeSetProjectName(url, applicationModel, null);

            // ApplicationModel parameter should take priority
            assertEquals(appModelName, System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
        } finally {
            fm2.destroy();
        }
    }

    @Test
    void testUrlApplicationTakesPriorityOverScopeModel() {
        // When ApplicationModel is null but URL has both application param and ScopeModel,
        // URL application param should take priority
        String urlAppName = "url-app";
        String scopeModelName = "scope-model-name";

        ApplicationConfig appConfig = new ApplicationConfig(scopeModelName);
        applicationModel.getApplicationConfigManager().setApplication(appConfig);

        URL url =
                URL.valueOf("nacos://127.0.0.1:8848?" + NACOS_SET_PROJECT_NAME_KEY + "=true&application=" + urlAppName);
        url = url.setScopeModel(applicationModel);

        NacosAppNameUtils.maybeSetProjectName(url, null, null);

        // URL application param should take priority over ScopeModel
        assertEquals(urlAppName, System.getProperty(PROJECT_NAME_SYS_PROP_KEY));
    }
}
