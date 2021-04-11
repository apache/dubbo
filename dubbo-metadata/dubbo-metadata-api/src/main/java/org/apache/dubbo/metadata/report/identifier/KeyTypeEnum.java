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
import static org.apache.dubbo.common.utils.StringUtils.EMPTY_STRING;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;
import static org.apache.dubbo.metadata.MetadataConstants.KEY_SEPARATOR;

/**
 * 2019-08-15
 */
public enum KeyTypeEnum {

    PATH(PATH_SEPARATOR) {
        public String build(String one, String... others) {
            return buildPath(one, others);
        }
    },

    UNIQUE_KEY(KEY_SEPARATOR) {
        public String build(String one, String... others) {
            StringBuilder keyBuilder = new StringBuilder(one);
            for (String other : others) {
                keyBuilder.append(separator).append(isBlank(other) ? EMPTY_STRING : other);
            }
            return keyBuilder.toString();
        }
    };

    final String separator;

    KeyTypeEnum(String separator) {
        this.separator = separator;
    }

    /**
     * Build Key
     *
     * @param one    one
     * @param others the others
     * @return
     * @since 2.7.8
     */
    public abstract String build(String one, String... others);

}
