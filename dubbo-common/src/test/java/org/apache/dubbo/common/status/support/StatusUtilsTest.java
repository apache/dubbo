/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.common.status.support;

import org.apache.dubbo.common.status.Status;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

class StatusUtilsTest {
    @Test
    void testGetSummaryStatus1() throws Exception {
        Status status1 = new Status(Status.Level.ERROR);
        Status status2 = new Status(Status.Level.WARN);
        Status status3 = new Status(Status.Level.OK);
        Map<String, Status> statuses = new HashMap<String, Status>();
        statuses.put("status1", status1);
        statuses.put("status2", status2);
        statuses.put("status3", status3);
        Status status = StatusUtils.getSummaryStatus(statuses);
        assertThat(status.getLevel(), is(Status.Level.ERROR));
        assertThat(status.getMessage(), containsString("status1"));
        assertThat(status.getMessage(), containsString("status2"));
        assertThat(status.getMessage(), not(containsString("status3")));
    }

    @Test
    void testGetSummaryStatus2() throws Exception {
        Status status1 = new Status(Status.Level.WARN);
        Status status2 = new Status(Status.Level.OK);
        Map<String, Status> statuses = new HashMap<String, Status>();
        statuses.put("status1", status1);
        statuses.put("status2", status2);
        Status status = StatusUtils.getSummaryStatus(statuses);
        assertThat(status.getLevel(), is(Status.Level.WARN));
        assertThat(status.getMessage(), containsString("status1"));
        assertThat(status.getMessage(), not(containsString("status2")));
    }

    @Test
    void testGetSummaryStatus3() throws Exception {
        Status status1 = new Status(Status.Level.OK);
        Map<String, Status> statuses = new HashMap<String, Status>();
        statuses.put("status1", status1);
        Status status = StatusUtils.getSummaryStatus(statuses);
        assertThat(status.getLevel(), is(Status.Level.OK));
        assertThat(status.getMessage(), isEmptyOrNullString());
    }
}