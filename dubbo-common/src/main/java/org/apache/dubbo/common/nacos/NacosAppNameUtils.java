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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

/**
 * Helper to bridge Dubbo application name to nacos-client app name inference.
 * <p>
 * Nacos Subscriber List shows application name based on nacos-client's {@code project.name}
 * system property. Dubbo registry/configcenter/metadata modules only pass parameters filtered by
 * {@code PropertyKeyConst}, which does not include any app name key in nacos-client 2.x. This helper
 * optionally maps Dubbo application name to {@code project.name} when explicitly enabled.
 */
public final class NacosAppNameUtils {

    public static final String NACOS_SET_PROJECT_NAME_KEY = "nacos.set-project-name";

    public static final String PROJECT_NAME_SYS_PROP_KEY = "project.name";

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(NacosAppNameUtils.class);

    private NacosAppNameUtils() {}

    /**
     * Best-effort mapping from Dubbo application name to nacos-client "project.name" system property.
     *
     * @param url              registry/config/metadata URL
     * @param applicationModel optional application model; if null, will resolve from URL scope model
     * @param customLogger     optional logger to emit info logs; fallback to internal logger when null
     */
    public static void maybeSetProjectName(
            URL url, ApplicationModel applicationModel, ErrorTypeAwareLogger customLogger) {
        if (url == null) {
            return;
        }
        boolean enabled = url.getParameter(NACOS_SET_PROJECT_NAME_KEY, false);
        if (!enabled) {
            return;
        }
        if (StringUtils.isNotEmpty(System.getProperty(PROJECT_NAME_SYS_PROP_KEY))) {
            return;
        }
        try {
            String appName = null;

            // Priority 1: explicitly passed ApplicationModel
            if (applicationModel != null) {
                appName = applicationModel.getApplicationName();
            }

            // Priority 2: URL's application parameter
            if (StringUtils.isEmpty(appName)) {
                appName = url.getApplication();
            }

            // Priority 3: resolve from URL's ScopeModel (may return default model with "unknown" name, so check last)
            if (StringUtils.isEmpty(appName)) {
                ApplicationModel model = ScopeModelUtil.getOrNullApplicationModel(url.getScopeModel());
                if (model != null) {
                    appName = model.getApplicationName();
                }
            }

            if (StringUtils.isEmpty(appName)) {
                return;
            }
            System.setProperty(PROJECT_NAME_SYS_PROP_KEY, appName);
            ErrorTypeAwareLogger log = customLogger == null ? logger : customLogger;
            log.info(
                    "Set system property '{}' to '{}' for Nacos subscriber application name.",
                    PROJECT_NAME_SYS_PROP_KEY,
                    appName);
        } catch (Throwable t) {
            // Log at debug level to aid troubleshooting, but do not impact Nacos client initialization
            ErrorTypeAwareLogger log = customLogger == null ? logger : customLogger;
            log.debug(
                    "Failed to set system property '{}' for Nacos subscriber application name. "
                            + "This is non-fatal and can be ignored if Nacos client works as expected.",
                    PROJECT_NAME_SYS_PROP_KEY,
                    t);
        }
    }
}
