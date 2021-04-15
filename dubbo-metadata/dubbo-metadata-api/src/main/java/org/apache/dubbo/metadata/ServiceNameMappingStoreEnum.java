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

package org.apache.dubbo.metadata;

import java.util.HashMap;
import java.util.Map;

public enum ServiceNameMappingStoreEnum {

    INTERFACE_APPLICATION_STORAGE("interface_application_storage"),
    APPLICANT_INTERFACE_STORAGE("applicant_interface_storage"),
    BOTH_STORAGE("both_storage");

    private String storeType;

    private static final Map<String, ServiceNameMappingStoreEnum> storeMap = new HashMap<>();

    static {
        for (ServiceNameMappingStoreEnum serviceNameMappingStoreEnum : values()) {
            storeMap.put(serviceNameMappingStoreEnum.getStoreType(), serviceNameMappingStoreEnum);
        }
    }


    ServiceNameMappingStoreEnum(String storeType) {
        this.storeType = storeType;
    }

    public String getStoreType() {
        return this.storeType;
    }

    public static ServiceNameMappingStoreEnum getStoreEnumByStoreType(String storeType) {
        return storeMap.get(storeType);
    }
}
