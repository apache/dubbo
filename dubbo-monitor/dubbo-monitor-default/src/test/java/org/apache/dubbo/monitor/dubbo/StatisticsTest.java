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
import org.apache.dubbo.monitor.MonitorService;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class StatisticsTest {
    @Test
    public void testEquals() {
        URL statistics = new URL("dubbo", "10.20.153.10", 0)
                .addParameter(MonitorService.APPLICATION, "morgan")
                .addParameter(MonitorService.INTERFACE, "MemberService")
                .addParameter(MonitorService.METHOD, "findPerson")
                .addParameter(MonitorService.CONSUMER, "10.20.153.11")
                .addParameter(MonitorService.SUCCESS, 1)
                .addParameter(MonitorService.FAILURE, 0)
                .addParameter(MonitorService.ELAPSED, 3)
                .addParameter(MonitorService.MAX_ELAPSED, 3)
                .addParameter(MonitorService.CONCURRENT, 1)
                .addParameter(MonitorService.MAX_CONCURRENT, 1);

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
    public void testToString() {
        Statistics statistics = new Statistics(new URL("dubbo", "10.20.153.10", 0));
        statistics.setApplication("demo");
        statistics.setMethod("findPerson");
        statistics.setServer("10.20.153.10");
        statistics.setGroup("unit-test");
        statistics.setService("MemberService");
        assertThat(statistics.toString(), is("dubbo://10.20.153.10"));

        Statistics statisticsWithDetailInfo = new Statistics(new URL("dubbo", "10.20.153.10", 0)
                .addParameter(MonitorService.APPLICATION, "morgan")
                .addParameter(MonitorService.INTERFACE, "MemberService")
                .addParameter(MonitorService.METHOD, "findPerson")
                .addParameter(MonitorService.CONSUMER, "10.20.153.11")
                .addParameter(MonitorService.GROUP, "unit-test")
                .addParameter(MonitorService.SUCCESS, 1)
                .addParameter(MonitorService.FAILURE, 0)
                .addParameter(MonitorService.ELAPSED, 3)
                .addParameter(MonitorService.MAX_ELAPSED, 3)
                .addParameter(MonitorService.CONCURRENT, 1)
                .addParameter(MonitorService.MAX_CONCURRENT, 1));

        MatcherAssert.assertThat(statisticsWithDetailInfo.getServer(), equalTo(statistics.getServer()));
        MatcherAssert.assertThat(statisticsWithDetailInfo.getService(), equalTo(statistics.getService()));
        MatcherAssert.assertThat(statisticsWithDetailInfo.getMethod(), equalTo(statistics.getMethod()));

        MatcherAssert.assertThat(statisticsWithDetailInfo.getGroup(), equalTo(statistics.getGroup()));
        MatcherAssert.assertThat(statisticsWithDetailInfo, not(equalTo(statistics)));
    }
}