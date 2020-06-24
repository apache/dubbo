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

import static org.apache.dubbo.common.constants.CommonConstants.COMPOSITE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.REMOTE_METADATA_STORAGE_TYPE;

/**
 * The type enumerations of {@link MetadataService}
 *
 * @see MetadataService
 * @since 2.7.8
 */
public enum MetadataServiceType {

    /**
     * The default type of {@link MetadataService}
     */
    DEFAULT(DEFAULT_METADATA_STORAGE_TYPE),

    /**
     * The remote type of {@link MetadataService}
     */
    REMOTE(REMOTE_METADATA_STORAGE_TYPE),

    /**
     * The composite type of {@link MetadataService}
     */
    COMPOSITE(COMPOSITE_METADATA_STORAGE_TYPE);

    /**
     * The {@link String} value of type
     */
    private final String value;

    MetadataServiceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MetadataServiceType getOrDefault(String value) {
        MetadataServiceType targetType = null;
        for (MetadataServiceType type : values()) {
            if (type.getValue().equals(value)) {
                targetType = type;
                break;
            }
        }
        if (targetType == null) {
            targetType = DEFAULT;
        }
        return targetType;
    }
}
