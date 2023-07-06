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
import org.apache.dubbo.metrics.model.key.MetricsKey;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_NAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_APPLICATION_VERSION_KEY;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_HOSTNAME;
import static org.apache.dubbo.common.constants.MetricsConstants.TAG_IP;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;
import static org.apache.dubbo.common.utils.NetUtils.getLocalHostName;

public class ApplicationMetric implements Metric {
    private final ApplicationModel applicationModel;
    private static final String version = Version.getVersion();
    private static final String commitId = Version.getLastCommitId();

    public ApplicationMetric(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    public String getApplicationName() {
        return getApplicationModel().getApplicationName();
    }

    public String getData() {
        return version;
    }

    @Override
    public Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put(TAG_IP, getLocalHost());
        tags.put(TAG_HOSTNAME, getLocalHostName());
        tags.put(TAG_APPLICATION_NAME, getApplicationName());
        tags.put(TAG_APPLICATION_VERSION_KEY, version);
        tags.put(MetricsKey.METADATA_GIT_COMMITID_METRIC.getName(), commitId);
        return tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationMetric)) return false;
        ApplicationMetric that = (ApplicationMetric) o;
        return Objects.equals(getApplicationName(), that.applicationModel.getApplicationName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApplicationName());
    }
}
