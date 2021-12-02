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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.constants.CommonConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum TripleHeaderEnum {

    AUTHORITY_KEY(":authority"),
    PATH_KEY(":path"),
    HTTP_STATUS_KEY("http-status"),
    STATUS_KEY("grpc-status"),
    MESSAGE_KEY("grpc-message"),
    STATUS_DETAIL_KEY("grpc-status-details-bin"),
    TIMEOUT("grpc-timeout"),
    CONTENT_TYPE_KEY("content-type"),
    CONTENT_PROTO("application/grpc+proto"),
    APPLICATION_GRPC("application/grpc"),
    GRPC_ENCODING("grpc-encoding"),
    GRPC_ACCEPT_ENCODING("grpc-accept-encoding"),
    CONSUMER_APP_NAME_KEY("tri-consumer-appname"),
    SERVICE_VERSION("tri-service-version"),
    SERVICE_GROUP("tri-service-group");

    static Map<String, TripleHeaderEnum> enumMap = new HashMap<>();

    static Set<String> excludeAttachmentsSet = new HashSet<>();

    static {
        for (TripleHeaderEnum item : TripleHeaderEnum.values()) {
            enumMap.put(item.getHeader(), item);
        }
        excludeAttachmentsSet.add(CommonConstants.GROUP_KEY);
        excludeAttachmentsSet.add(CommonConstants.INTERFACE_KEY);
        excludeAttachmentsSet.add(CommonConstants.PATH_KEY);
        excludeAttachmentsSet.add(CommonConstants.REMOTE_APPLICATION_KEY);
        excludeAttachmentsSet.add(CommonConstants.APPLICATION_KEY);
        excludeAttachmentsSet.add(TripleConstant.SERIALIZATION_KEY);
        excludeAttachmentsSet.add(TripleConstant.TE_KEY);
    }

    private final String header;

    TripleHeaderEnum(String header) {
        this.header = header;
    }

    public static TripleHeaderEnum getEnum(String header) {
        return enumMap.get(header);
    }

    public static boolean contains(String header) {
        return enumMap.containsKey(header);
    }

    public static boolean containsExcludeAttachments(String key) {
        return excludeAttachmentsSet.contains(key) || enumMap.containsKey(key);
    }

    public String getHeader() {
        return header;
    }
}
