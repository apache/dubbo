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

class CorsUtilsTest {

    //    @Test
    //    void testResolveGlobalMetaInCommon() {
    //        Configuration config = Mockito.mock(Configuration.class);
    //        Mockito.when(config.getString(RestConstants.ALLOWED_ORIGINS)).thenReturn("http://localhost:8080");
    //        Mockito.when(config.getString(RestConstants.ALLOWED_METHODS)).thenReturn("GET,POST,PUT,DELETE");
    //        Mockito.when(config.getString(RestConstants.ALLOWED_HEADERS)).thenReturn("Content-Type,Authorization");
    //        Mockito.when(config.getString(RestConstants.EXPOSED_HEADERS)).thenReturn("Content-Type,Authorization");
    //        Mockito.when(config.getString(RestConstants.MAX_AGE)).thenReturn("3600");
    //        Mockito.when(config.getString(RestConstants.MAX_AGE)).thenReturn("3600");
    //        FrameworkModel frameworkModel = Mockito.mock(FrameworkModel.class);
    //
    //        CorsMeta meta = CorsUtils.getGlobalCorsMeta();
    //        Assertions.assertTrue(meta.getAllowedOrigins().contains("http://localhost:8080"));
    //        Assertions.assertTrue(meta.getAllowedMethods().contains("GET"));
    //        Assertions.assertTrue(meta.getAllowedMethods().contains("POST"));
    //        Assertions.assertTrue(meta.getAllowedMethods().contains("PUT"));
    //        Assertions.assertTrue(meta.getAllowedMethods().contains("DELETE"));
    //        Assertions.assertTrue(meta.getAllowedHeaders().contains("Content-Type"));
    //        Assertions.assertTrue(meta.getAllowedHeaders().contains("Authorization"));
    //        Assertions.assertTrue(meta.getExposedHeaders().contains("Content-Type"));
    //        Assertions.assertTrue(meta.getExposedHeaders().contains("Authorization"));
    //        Assertions.assertEquals(3600, meta.getMaxAge());
    //    }
    //
    //    @Test
    //    void testResolveGlobalMetaWithNullConfig() {
    //        Configuration config = Mockito.mock(Configuration.class);
    //        Mockito.when(config.getString(RestConstants.ALLOWED_ORIGINS)).thenReturn(null);
    //        Mockito.when(config.getString(RestConstants.ALLOWED_METHODS)).thenReturn(null);
    //        Mockito.when(config.getString(RestConstants.ALLOWED_HEADERS)).thenReturn(null);
    //        Mockito.when(config.getString(RestConstants.EXPOSED_HEADERS)).thenReturn(null);
    //        Mockito.when(config.getString(RestConstants.MAX_AGE)).thenReturn(null);
    //        CorsMeta meta = CorsUtils.getGlobalCorsMeta()
    //        Assertions.assertNull(meta.getMaxAge());
    //        Assertions.assertNull(meta.getAllowedOrigins());
    //        Assertions.assertNull(meta.getAllowedMethods());
    //        Assertions.assertNull(meta.getAllowedHeaders());
    //    }

}