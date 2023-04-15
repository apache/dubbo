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

package org.apache.dubbo.metrics.model;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.metrics.exception.MetricsNeverHappenException;
import org.apache.dubbo.metrics.model.key.MetricsKey;

import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_INTERFACE_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

public class MetricsSupport {

    private static final String version = Version.getVersion();
    private static final String commitId = Version.getLastCommitId();

    public static Map<String, String> applicationTags(String applicationName) {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, applicationName);
        tags.put(TAG_APPLICATION_VERSION_KEY, version);
        tags.put(MetricsKey.METADATA_GIT_COMMITID_METRIC.getName(), commitId);
        return tags;
    }

    public static Map<String, String> serviceTags(String appAndServiceName) {
        String[] keys = appAndServiceName.split("_");
        if (keys.length != 2) {
            throw new MetricsNeverHappenException("Error service name: " + appAndServiceName);
        }
        Map<String, String> tags = applicationTags(keys[0]);
        tags.put(TAG_INTERFACE_KEY, keys[1]);
        return tags;
    }
}
