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
package org.apache.dubbo.common.constants;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR_CHAR;
import static org.apache.dubbo.common.constants.CommonConstants.COMPOSITE_METADATA_STORAGE_TYPE;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_SERVICE_NAME_MAPPING_PROPERTIES_PATH;
import static org.apache.dubbo.common.constants.CommonConstants.SERVICE_NAME_MAPPING_PROPERTIES_FILE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CommonConstants} Test-Cases
 *
 * @since 2.7.8
 */
class CommonConstantsTest {

    @Test
    void test() {
        assertEquals(',', COMMA_SEPARATOR_CHAR);
        assertEquals("composite", COMPOSITE_METADATA_STORAGE_TYPE);
        assertEquals("service-name-mapping.properties-path", SERVICE_NAME_MAPPING_PROPERTIES_FILE_KEY);
        assertEquals("META-INF/dubbo/service-name-mapping.properties", DEFAULT_SERVICE_NAME_MAPPING_PROPERTIES_PATH);
    }
}