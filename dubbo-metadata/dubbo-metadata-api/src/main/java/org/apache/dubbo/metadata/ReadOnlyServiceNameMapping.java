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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR_CHAR;
import static org.apache.dubbo.common.utils.StringUtils.splitToSet;

/**
 * Read-Only implementation of {@link ServiceNameMapping}
 *
 * @since 2.7.8
 */
public abstract class ReadOnlyServiceNameMapping implements ServiceNameMapping {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void map(URL exportedURL) {
        // DO NOTING for mapping
    }

    protected Set<String> getValue(String rawValue) {
        return splitToSet(rawValue, COMMA_SEPARATOR_CHAR, true);
    }
}
