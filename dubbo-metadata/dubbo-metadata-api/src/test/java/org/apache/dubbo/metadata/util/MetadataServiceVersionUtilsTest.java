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
package org.apache.dubbo.metadata.util;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.apache.dubbo.common.constants.CommonConstants.METADATA_SERVICE_PROTOCOL_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetadataServiceVersionUtilsTest {

    static Stream<Arguments> metadataProtocolOverrides() {
        return Stream.of(
                Arguments.of(null, null, true),
                Arguments.of(null, Collections.emptyMap(), true),
                Arguments.of(null, Collections.singletonMap("unrelated", "value"), true),
                Arguments.of(null, Collections.singletonMap(METADATA_SERVICE_PROTOCOL_KEY, ""), true),
                Arguments.of(null, Collections.singletonMap(METADATA_SERVICE_PROTOCOL_KEY, "tri"), true),
                Arguments.of(null, Collections.singletonMap(METADATA_SERVICE_PROTOCOL_KEY, "dubbo"), false),
                Arguments.of("", Collections.singletonMap(METADATA_SERVICE_PROTOCOL_KEY, "dubbo"), false),
                Arguments.of("tri", Collections.singletonMap(METADATA_SERVICE_PROTOCOL_KEY, "dubbo"), true),
                Arguments.of("dubbo", Collections.singletonMap(METADATA_SERVICE_PROTOCOL_KEY, "tri"), false));
    }

    @ParameterizedTest
    @MethodSource("metadataProtocolOverrides")
    void metadataProtocolOverridesConfiguredTriple(
            String metadataProtocol, Map<String, String> parameters, boolean expectedTriple) {
        ApplicationConfig application = new ApplicationConfig("metadata-protocol-configuration");
        application.setMetadataServiceProtocol(metadataProtocol);
        application.setParameters(parameters);
        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplication()).thenReturn(Optional.of(application));
        when(configManager.getProtocols()).thenReturn(Collections.singletonList(new ProtocolConfig("tri")));
        ApplicationModel applicationModel = mock(ApplicationModel.class);
        when(applicationModel.getApplicationConfigManager()).thenReturn(configManager);

        assertEquals(expectedTriple, MetadataServiceVersionUtils.tripleConfigured(applicationModel));
    }

    @Test
    void configuredProtocolIsUsedWithoutApplicationConfig() {
        ConfigManager configManager = mock(ConfigManager.class);
        when(configManager.getApplication()).thenReturn(Optional.empty());
        when(configManager.getProtocols()).thenReturn(Collections.singletonList(new ProtocolConfig("tri")));
        ApplicationModel applicationModel = mock(ApplicationModel.class);
        when(applicationModel.getApplicationConfigManager()).thenReturn(configManager);

        assertTrue(MetadataServiceVersionUtils.tripleConfigured(applicationModel));
    }
}
