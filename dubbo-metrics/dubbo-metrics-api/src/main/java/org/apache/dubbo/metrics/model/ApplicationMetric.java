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

import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.Objects;

public class ApplicationMetric implements Metric {
    private final ApplicationModel applicationModel;
    protected Map<String, String> extraInfo;

    public ApplicationMetric(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    public String getApplicationName() {
        return getApplicationModel().getApplicationName();
    }

    @Override
    public Map<String, String> getTags() {
        return hostTags(gitTags(MetricsSupport.applicationTags(applicationModel, getExtraInfo())));
    }

    public Map<String, String> gitTags(Map<String, String> tags) {
        return MetricsSupport.gitTags(tags);
    }

    public Map<String, String> hostTags(Map<String, String> tags) {
        return MetricsSupport.hostTags(tags);
    }

    public Map<String, String> getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(Map<String, String> extraInfo) {
        this.extraInfo = extraInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ApplicationMetric that = (ApplicationMetric) o;
        return getApplicationName().equals(that.applicationModel.getApplicationName())
                && Objects.equals(extraInfo, that.extraInfo);
    }

    private volatile int hashCode;

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Objects.hash(getApplicationName(), extraInfo);
        }
        return hashCode;
    }
}
