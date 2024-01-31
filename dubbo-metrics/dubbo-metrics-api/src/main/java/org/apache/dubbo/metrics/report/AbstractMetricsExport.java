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
package org.apache.dubbo.metrics.report;

import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.rpc.model.ApplicationModel;

/**
 * Store public information such as application
 */
public abstract class AbstractMetricsExport implements MetricsExport {

    private volatile Boolean serviceLevel;

    private final ApplicationModel applicationModel;

    public AbstractMetricsExport(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    public ApplicationModel getApplicationModel() {
        return applicationModel;
    }

    public String getAppName() {
        return getApplicationModel().getApplicationName();
    }

    protected boolean getServiceLevel() {
        initServiceLevelConfig();
        return this.serviceLevel;
    }

    private void initServiceLevelConfig() {
        if (serviceLevel == null) {
            synchronized (this) {
                if (serviceLevel == null) {
                    this.serviceLevel = MethodMetric.isServiceLevel(getApplicationModel());
                }
            }
        }
    }
}
