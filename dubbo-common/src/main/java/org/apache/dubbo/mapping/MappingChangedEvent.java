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
package org.apache.dubbo.mapping;

import java.util.Set;

public class MappingChangedEvent {

    //before mapping model, one key -> multi value
    public static final int BEFORE = 0;

    //the new mapping model, one key -> one value. use cas to ensure the data thread safe
    public static final int CAS = 1;

    private final String serviceKey;
    private final Set<String> apps;
    private final int type;

    private MappingChangedEvent(String serviceKey, Set<String> apps, int type) {
        this.serviceKey = serviceKey;
        this.apps = apps;
        this.type = type;
    }

    public boolean isNewModel() {
        return type == CAS;
    }

    public static MappingChangedEvent buildOldModelEvent(String serviceKey, Set<String> apps) {
        return new MappingChangedEvent(serviceKey, apps, BEFORE);
    }

    public static MappingChangedEvent buildCasModelEvent(String serviceKey, Set<String> apps) {
        return new MappingChangedEvent(serviceKey, apps, CAS);
    }


    public String getServiceKey() {
        return serviceKey;
    }

    public Set<String> getApps() {
        return apps;
    }

    @Override
    public String toString() {
        return "{serviceKey: " + serviceKey + ", apps: " +
                apps.toString() + "}";
    }
}
