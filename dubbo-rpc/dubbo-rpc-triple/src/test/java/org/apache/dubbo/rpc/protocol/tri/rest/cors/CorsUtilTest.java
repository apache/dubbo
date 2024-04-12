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
package org.apache.dubbo.rpc.protocol.tri.rest.cors;

import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.rpc.protocol.tri.rest.RestConstants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.apache.dubbo.rpc.protocol.tri.rest.RestConstants.CORS_CONFIG_PREFIX;

public class CorsUtilTest {

    @Test
    void testResolveGlobalMetaInCommon() {
        Configuration config = Mockito.mock(Configuration.class);
        Mockito.when(config.getString(RestConstants.ALLOWED_ORIGINS)).thenReturn("http://localhost:8080");
        Mockito.when(config.getString(RestConstants.ALLOWED_METHODS)).thenReturn("GET,POST,PUT,DELETE");
        Mockito.when(config.getString(RestConstants.ALLOWED_HEADERS)).thenReturn("Content-Type,Authorization");
        Mockito.when(config.getString(RestConstants.EXPOSED_HEADERS)).thenReturn("Content-Type,Authorization");
        Mockito.when(config.getString(RestConstants.MAX_AGE)).thenReturn("3600");
        Mockito.when(config.getString(RestConstants.ALLOW_CREDENTIALS)).thenReturn("true");
        Mockito.when(config.getString(RestConstants.ALLOW_PRIVATE_NETWORK)).thenReturn("true");
        Mockito.when(config.getString(CORS_CONFIG_PREFIX + RestConstants.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK))
                .thenReturn("true");
        CorsMeta meta = CorsUtil.resolveGlobalMeta(config);
        Assertions.assertTrue(meta.getAllowedOrigins().contains("http://localhost:8080"));
        Assertions.assertTrue(meta.getAllowedMethods().contains("GET"));
        Assertions.assertTrue(meta.getAllowedMethods().contains("POST"));
        Assertions.assertTrue(meta.getAllowedMethods().contains("PUT"));
        Assertions.assertTrue(meta.getAllowedMethods().contains("DELETE"));
        Assertions.assertTrue(meta.getAllowedHeaders().contains("Content-Type"));
        Assertions.assertTrue(meta.getAllowedHeaders().contains("Authorization"));
        Assertions.assertTrue(meta.getExposedHeaders().contains("Content-Type"));
        Assertions.assertTrue(meta.getExposedHeaders().contains("Authorization"));
        Assertions.assertEquals(3600, meta.getMaxAge());
        Assertions.assertTrue(meta.getAllowCredentials());
        Assertions.assertTrue(meta.getAllowPrivateNetwork());
    }

    @Test
    void testResolveGlobalMetaWithNullConfig() {
        Configuration config = Mockito.mock(Configuration.class);
        Mockito.when(config.getString(RestConstants.ALLOWED_ORIGINS)).thenReturn(null);
        Mockito.when(config.getString(RestConstants.ALLOWED_METHODS)).thenReturn(null);
        Mockito.when(config.getString(RestConstants.ALLOWED_HEADERS)).thenReturn(null);
        Mockito.when(config.getString(RestConstants.EXPOSED_HEADERS)).thenReturn(null);
        Mockito.when(config.getString(RestConstants.MAX_AGE)).thenReturn(null);
        Mockito.when(config.getString(RestConstants.ALLOW_CREDENTIALS)).thenReturn(null);
        Mockito.when(config.getString(RestConstants.ALLOW_PRIVATE_NETWORK)).thenReturn(null);

        CorsMeta meta = CorsUtil.resolveGlobalMeta(config);
        Assertions.assertEquals(meta.getMaxAge(), CorsMeta.DEFAULT_MAX_AGE);
        Assertions.assertEquals(meta.getAllowCredentials(), false);
        Assertions.assertEquals(meta.getAllowPrivateNetwork(), false);
        Assertions.assertEquals(meta.getAllowedOrigins(), CorsMeta.DEFAULT_PERMIT_ALL);
        Assertions.assertEquals(meta.getAllowedMethods(), CorsMeta.DEFAULT_PERMIT_METHODS);
        Assertions.assertEquals(meta.getAllowedHeaders(), CorsMeta.DEFAULT_PERMIT_ALL);
    }

    @Test
    void testGetPortWithDefaultValues() {
        Assertions.assertEquals(80, CorsUtil.getPort(RestConstants.HTTP, -1));
        Assertions.assertEquals(80, CorsUtil.getPort(RestConstants.WS, -1));
        Assertions.assertEquals(443, CorsUtil.getPort(RestConstants.HTTPS, -1));
        Assertions.assertEquals(443, CorsUtil.getPort(RestConstants.WSS, -1));
    }

    @Test
    void testGetPortWithCustomValues() {
        Assertions.assertEquals(8080, CorsUtil.getPort(RestConstants.HTTP, 8080));
        Assertions.assertEquals(8443, CorsUtil.getPort(RestConstants.HTTPS, 8443));
    }
}
