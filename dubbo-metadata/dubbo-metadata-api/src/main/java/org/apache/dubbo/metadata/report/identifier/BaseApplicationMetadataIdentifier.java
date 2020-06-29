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

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.utils.PathUtils.buildPath;
import static org.apache.dubbo.metadata.MetadataConstants.DEFAULT_PATH_TAG;
import static org.apache.dubbo.metadata.MetadataConstants.KEY_SEPARATOR;

/**
 * The Base class of MetadataIdentifier for service scope
 * <p>
 * 2019-08-09
 */
public class BaseApplicationMetadataIdentifier {
    String application;

    String getUniqueKey(KeyTypeEnum keyType, String... params) {
        if (keyType == KeyTypeEnum.PATH) {
            return getFilePathKey(params);
        }
        return getIdentifierKey(params);
    }

    String getIdentifierKey(String... params) {
        return application + joinParams(KEY_SEPARATOR, params);
    }

    private String joinParams(String joinChar, String... params) {
        if (params == null || params.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String param : params) {
            sb.append(joinChar);
            sb.append(param);
        }
        return sb.toString();
    }

    private String getFilePathKey(String... params) {
        return getFilePathKey(DEFAULT_PATH_TAG, params);
    }

    private String getFilePathKey(String pathTag, String... params) {
        return buildPath(pathTag, application, joinParams(PATH_SEPARATOR, params));
    }

}
