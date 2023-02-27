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
package org.apache.dubbo.metadata.report.identifier;

import org.apache.dubbo.common.URL;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_PATH_TAG;

/**
 * The Base class of MetadataIdentifier for service scope
 * <p>
 * 2019-08-09
 */
public class BaseServiceMetadataIdentifier {
    protected String serviceInterface;
    protected String version;
    protected String group;
    protected String side;

    protected String getUniqueKey(KeyTypeEnum keyType, String... params) {
        if (keyType == KeyTypeEnum.PATH) {
            return getFilePathKey(params);
        }
        return getIdentifierKey(params);
    }

    protected String getIdentifierKey(String... params) {
        String prefix = KeyTypeEnum.UNIQUE_KEY.build(serviceInterface, version, group, side);
        return KeyTypeEnum.UNIQUE_KEY.build(prefix, params);
    }

    private String getFilePathKey(String... params) {
        return getFilePathKey(DEFAULT_PATH_TAG, params);
    }

    private String getFilePathKey(String pathTag, String... params) {
        String prefix = KeyTypeEnum.PATH.build(pathTag, toServicePath(), version, group, side);
        return KeyTypeEnum.PATH.build(prefix, params);
    }

    private String toServicePath() {
        if (ANY_VALUE.equals(serviceInterface)) {
            return "";
        }
        return URL.encode(serviceInterface);
    }
}
