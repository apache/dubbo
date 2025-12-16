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
 *
 * <h3>Thread Safety</h3>
 * <p>
 * This utility uses a best-effort approach and is <b>not strictly thread-safe</b>. There is a potential
 * race condition between checking whether the system property exists and setting it. If multiple threads
 * concurrently initialize Nacos connections (e.g., registry, config-center, and metadata-report), they may
 * all pass the existence check and race to set the property. In practice, this is benign because:
 * <ul>
 *   <li>All competing threads typically resolve to the same application name</li>
 *   <li>The property is only used for display purposes in Nacos Subscriber List</li>
 *   <li>Once set, subsequent calls will see the property and skip the set operation</li>
 * </ul>
 * <p>
 * If strict thread-safety is required, callers should synchronize externally or set the
 * {@code project.name} system property before initializing any Nacos connections.
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
            String appName = getApplicationName(url, applicationModel);
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

    /**
     * Resolves the application name from multiple sources with the following priority:
     * <ol>
     *   <li>Explicitly passed ApplicationModel</li>
     *   <li>URL's application parameter</li>
     *   <li>URL's ScopeModel (may return default model with "unknown" name, so checked last)</li>
     * </ol>
     *
     * @param url              registry/config/metadata URL (must not be null)
     * @param applicationModel optional application model
     * @return the resolved application name, or null if not found from any source
     */
    private static String getApplicationName(URL url, ApplicationModel applicationModel) {
        // Priority 1: explicitly passed ApplicationModel
        if (applicationModel != null) {
            String appName = applicationModel.getApplicationName();
            if (StringUtils.isNotEmpty(appName)) {
                return appName;
            }
        }

        // Priority 2: URL's application parameter
        String appName = url.getApplication();
        if (StringUtils.isNotEmpty(appName)) {
            return appName;
        }

        // Priority 3: resolve from URL's ScopeModel
        ApplicationModel model = ScopeModelUtil.getOrNullApplicationModel(url.getScopeModel());
        if (model != null) {
            return model.getApplicationName();
        }

        return null;
    }
}
