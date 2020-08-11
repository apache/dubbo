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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.MetadataReportConfig;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class MetadataReportBuilderTest {

    @Test
    void address() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.address("address");
        Assertions.assertEquals("address", builder.build().getAddress());
    }

    @Test
    void username() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.username("username");
        Assertions.assertEquals("username", builder.build().getUsername());
    }

    @Test
    void password() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.password("password");
        Assertions.assertEquals("password", builder.build().getPassword());
    }

    @Test
    void timeout() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.timeout(1000);
        Assertions.assertEquals(1000, builder.build().getTimeout());
    }

    @Test
    void group() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.group("group");
        Assertions.assertEquals("group", builder.build().getGroup());
    }

    @Test
    void appendParameter() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.appendParameter("default.num", "one").appendParameter("num", "ONE");

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void appendParameters() {
        Map<String, String> source = new HashMap<>();
        source.put("default.num", "one");
        source.put("num", "ONE");

        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.appendParameters(source);

        Map<String, String> parameters = builder.build().getParameters();

        Assertions.assertTrue(parameters.containsKey("default.num"));
        Assertions.assertEquals("ONE", parameters.get("num"));
    }

    @Test
    void retryTimes() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.retryTimes(1);
        Assertions.assertEquals(1, builder.build().getRetryTimes());
    }

    @Test
    void retryPeriod() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.retryPeriod(2);
        Assertions.assertEquals(2, builder.build().getRetryPeriod());
    }

    @Test
    void cycleReport() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.cycleReport(true);
        Assertions.assertTrue(builder.build().getCycleReport());
        builder.cycleReport(false);
        Assertions.assertFalse(builder.build().getCycleReport());
        builder.cycleReport(null);
        Assertions.assertNull(builder.build().getCycleReport());
    }

    @Test
    void syncReport() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.syncReport(true);
        Assertions.assertTrue(builder.build().getSyncReport());
        builder.syncReport(false);
        Assertions.assertFalse(builder.build().getSyncReport());
        builder.syncReport(null);
        Assertions.assertNull(builder.build().getSyncReport());
    }

    @Test
    void build() {
        MetadataReportBuilder builder = new MetadataReportBuilder();
        builder.address("address").username("username").password("password").timeout(1000).group("group")
                .retryTimes(1).retryPeriod(2).cycleReport(true).syncReport(false)
                .appendParameter("default.num", "one").id("id").prefix("prefix");

        MetadataReportConfig config = builder.build();
        MetadataReportConfig config2 = builder.build();

        Assertions.assertTrue(config.getCycleReport());
        Assertions.assertFalse(config.getSyncReport());
        Assertions.assertEquals(1000, config.getTimeout());
        Assertions.assertEquals(1, config.getRetryTimes());
        Assertions.assertEquals(2, config.getRetryPeriod());
        Assertions.assertEquals("address", config.getAddress());
        Assertions.assertEquals("username", config.getUsername());
        Assertions.assertEquals("password", config.getPassword());
        Assertions.assertEquals("group", config.getGroup());
        Assertions.assertTrue(config.getParameters().containsKey("default.num"));
        Assertions.assertEquals("one", config.getParameters().get("default.num"));
        Assertions.assertEquals("id", config.getId());
        Assertions.assertEquals("prefix", config.getPrefix());
        Assertions.assertNotSame(config, config2);
    }
}