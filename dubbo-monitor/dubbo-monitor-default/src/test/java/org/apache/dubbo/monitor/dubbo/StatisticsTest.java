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
package org.apache.dubbo.monitor.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.url.component.ServiceConfigURL;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_PROTOCOL;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.monitor.Constants.CONCURRENT_KEY;
import static org.apache.dubbo.monitor.Constants.ELAPSED_KEY;
import static org.apache.dubbo.monitor.Constants.FAILURE_KEY;
import static org.apache.dubbo.monitor.Constants.MAX_CONCURRENT_KEY;
import static org.apache.dubbo.monitor.Constants.MAX_ELAPSED_KEY;
import static org.apache.dubbo.monitor.Constants.SUCCESS_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class StatisticsTest {
    @Test
    void testEquals() {
        URL statistics = new URLBuilder(DUBBO_PROTOCOL, "10.20.153.10", 0)
                .addParameter(APPLICATION_KEY, "morgan")
                .addParameter(INTERFACE_KEY, "MemberService")
                .addParameter(METHOD_KEY, "findPerson")
                .addParameter(CONSUMER, "10.20.153.11")
                .addParameter(SUCCESS_KEY, 1)
                .addParameter(FAILURE_KEY, 0)
                .addParameter(ELAPSED_KEY, 3)
                .addParameter(MAX_ELAPSED_KEY, 3)
                .addParameter(CONCURRENT_KEY, 1)
                .addParameter(MAX_CONCURRENT_KEY, 1)
                .build();

        Statistics statistics1 = new Statistics(statistics);
        Statistics statistics2 = new Statistics(statistics);

        MatcherAssert.assertThat(statistics1, equalTo(statistics1));
        MatcherAssert.assertThat(statistics1, equalTo(statistics2));

        statistics1.setVersion("2");
        MatcherAssert.assertThat(statistics1, not(equalTo(statistics2)));
        MatcherAssert.assertThat(statistics1.hashCode(), not(equalTo(statistics2.hashCode())));

        statistics1.setMethod("anotherMethod");
        MatcherAssert.assertThat(statistics1, not(equalTo(statistics2)));
        MatcherAssert.assertThat(statistics1.hashCode(), not(equalTo(statistics2.hashCode())));

        statistics1.setClient("anotherClient");
        MatcherAssert.assertThat(statistics1, not(equalTo(statistics2)));
        MatcherAssert.assertThat(statistics1.hashCode(), not(equalTo(statistics2.hashCode())));
    }

    @Test
    void testToString() {
        Statistics statistics = new Statistics(new ServiceConfigURL("dubbo", "10.20.153.10", 0));
        statistics.setApplication("demo");
        statistics.setMethod("findPerson");
        statistics.setServer("10.20.153.10");
        statistics.setGroup("unit-test");
        statistics.setService("MemberService");
        assertThat(statistics.toString(), is("dubbo://10.20.153.10"));

        Statistics statisticsWithDetailInfo = new Statistics(new URLBuilder(DUBBO_PROTOCOL, "10.20.153.10", 0)
                .addParameter(APPLICATION_KEY, "morgan")
                .addParameter(INTERFACE_KEY, "MemberService")
                .addParameter(METHOD_KEY, "findPerson")
                .addParameter(CONSUMER, "10.20.153.11")
                .addParameter(GROUP_KEY, "unit-test")
                .addParameter(SUCCESS_KEY, 1)
                .addParameter(FAILURE_KEY, 0)
                .addParameter(ELAPSED_KEY, 3)
                .addParameter(MAX_ELAPSED_KEY, 3)
                .addParameter(CONCURRENT_KEY, 1)
                .addParameter(MAX_CONCURRENT_KEY, 1)
                .build());

        MatcherAssert.assertThat(statisticsWithDetailInfo.getServer(), equalTo(statistics.getServer()));
        MatcherAssert.assertThat(statisticsWithDetailInfo.getService(), equalTo(statistics.getService()));
        MatcherAssert.assertThat(statisticsWithDetailInfo.getMethod(), equalTo(statistics.getMethod()));

        MatcherAssert.assertThat(statisticsWithDetailInfo.getGroup(), equalTo(statistics.getGroup()));
        MatcherAssert.assertThat(statisticsWithDetailInfo, not(equalTo(statistics)));
    }
}