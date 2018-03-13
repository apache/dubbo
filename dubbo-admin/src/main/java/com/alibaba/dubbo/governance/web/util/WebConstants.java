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
package com.alibaba.dubbo.governance.web.util;

import java.util.Map;

/**
 * Contains the constants used in the web layer
 *
 */
public class WebConstants {

    /**
     * In the session to save the current user object's key.
     */
    public static final String CURRENT_USER_KEY = "currentUser";
    /**
     * The current registered server address
     */
    public static final String REGISTRY_ADDRESS = "registryAddress";
    /**
     * Service exposed address
     */
    public static final String SERVICE_URL = "serviceUrl";
    /**
     * Service name
     */
    public static final String SERVICE_NAME = "serviceName";
    /**
     * Service name
     */
    public static final String ENTRY = "entry";
    /**
     * buc sso logout
     */
    public static final String SSO_LOGOUT_URL = "SSO_LOGOUT_URL";
    /**
     * buc sso logon
     */
    public static final String BUC_SSO_USERNAME = "buc_sso_username";
    /**
     * Operation record page The default page record shows the number of records
     */
    public static final Integer OPRATION_RECORDS_PAGE_SIZE = 100;
    Map<String, Object> context;

}
