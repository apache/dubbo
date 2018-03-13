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
package com.alibaba.dubbo.registry.common.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Record error messages and record all user operations
 *
 */
public class Operation extends Entity {

    public static final String PROVIDER_TYPE = "Provider";
    public static final String CONSUMER_TYPE = "Consumer";
    public static final String ROUTE_TYPE = "Route";
    public static final String WEIGHT_TYPE = "Weight";
    public static final String CLUSTER_TYPE = "Cluster";
    public static final String DOCUMENT_TYPE = "Document";
    public static final String LOADBALANCE_TYPE = "LoadBalance";
    public static final String TEST_TYPE = "Test";
    public static final String MOCK_TYPE = "Mock";
    public static final String AGREEMENT_TYPE = "Agreement";
    public static final String APPROVAL_TYPE = "Approval";
    public static final String APPROVAL_REQUISITION_TYPE = "ApprovalRequisition";
    public static final String USER_TYPE = "User";
    public static final String FEATURE_TYPE = "Feature";
    public static final String LAYER_TYPE = "Layer";
    public static final String CONFIG_TYPE = "Config";
    public static final List<String> TYPES =
            Collections.unmodifiableList(Arrays.asList(new String[]{PROVIDER_TYPE, CONSUMER_TYPE, ROUTE_TYPE, WEIGHT_TYPE,
                    CLUSTER_TYPE, DOCUMENT_TYPE, LOADBALANCE_TYPE, TEST_TYPE,
                    AGREEMENT_TYPE, USER_TYPE, FEATURE_TYPE, CONFIG_TYPE}));
    private static final long serialVersionUID = 8220325876753890396L;
    private String username;     // operator

    private String dataType;     // data type, e.g., route, cluster

    private String operateType;    // operations, e.g. update, create

    private String data;       // record data information

    public Operation() {
    }

    public Operation(Long id) {
        super(id);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        if (data != null && data.length() > 1000) {
            data = data.substring(0, 1000);
        }
        this.data = data;
    }

    public String getOperateType() {
        return operateType;
    }

    public void setOperateType(String operateType) {
        this.operateType = operateType;
    }
}
