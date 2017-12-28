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

/**
 * Service change information object
 *
 */
public class Change extends Entity {

    public static final String PROVIDER_TYPE = "P";     //provider change
    public static final String CONSUMER_TYPE = "N";     //consumer change
    public static final String ROUTE_TYPE = "R";        //route change
    public static final String WEIGHT_TYPE = "W";       //weight change
    public static final String LOADBALANCE_TYPE = "L";  //loadbalance change
    public static final String CLUSTER_TYPE = "G";      //group change
    public static final String USER_TYPE = "U";         //user change
    public static final String CONFIG_TYPE = "C";       //system config change
    public static final String FEATURE_TYPE = "F";      //feature change
    public static final String LAYER_TYPE = "Y";      //layer change
    public static final String TEST_TYPE = "T";         //service test change
    public static final String MOCK_TYPE = "M";         //service mock change
    public static final String ACCESS_TYPE = "A";       //access change
    public static final String OVERRIDE_TYPE = "O";     //override change
    private static final long serialVersionUID = 15528419903956898L;
    private String type;                    /* type of change */

    private String service;                 /* service name */

    private long sequence;                /* NO. of change */

    private String data;                    /* what is changed */

    public Change() {
    }

    public Change(Long id) {
        super(id);
    }

    public Change(String type, String serviceName) {
        this.type = type;
        this.service = serviceName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Deprecated
    /**
     * us id as sequence
     */
    public long getSequence() {
        return sequence;
    }

    @Deprecated
    /**
     * use id as sequence
     */
    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
