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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_CHANGE_TYPE;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_CONFIG_CENTER;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_GROUP_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_KEY_KEY;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

public class ConfigCenterMetric implements Metric {

    private String applicationName;
    private String key;
    private String group;
    private String configCenter;
    private String changeType;

    public ConfigCenterMetric() {

    }

    public ConfigCenterMetric(String applicationName, String key, String group, String configCenter, String changeType) {
        this.applicationName = applicationName;
        this.key = key;
        this.group = group;
        this.configCenter = configCenter;
        this.changeType = changeType;
    }

    @Override
    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, applicationName);

        tags.put(TAG_KEY_KEY, key);
        tags.put(TAG_GROUP_KEY, group);
        tags.put(TAG_CONFIG_CENTER, configCenter);
        tags.put(TAG_CHANGE_TYPE, changeType.toLowerCase());

        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConfigCenterMetric that = (ConfigCenterMetric) o;

        if (!Objects.equals(applicationName, that.applicationName))
            return false;
        if (!Objects.equals(key, that.key)) return false;
        if (!Objects.equals(group, that.group)) return false;
        if (!Objects.equals(configCenter, that.configCenter)) return false;
        return Objects.equals(changeType, that.changeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationName, key, group, configCenter, changeType);
    }
}
