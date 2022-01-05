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

package org.apache.dubbo.common.status;

import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.status.Status.Level.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

public class StatusTest {
    @Test
    public void testConstructor1() throws Exception {
        Status status = new Status(OK, "message", "description");
        assertThat(status.getLevel(), is(OK));
        assertThat(status.getMessage(), equalTo("message"));
        assertThat(status.getDescription(), equalTo("description"));
    }

    @Test
    public void testConstructor2() throws Exception {
        Status status = new Status(OK, "message");
        assertThat(status.getLevel(), is(OK));
        assertThat(status.getMessage(), equalTo("message"));
        assertThat(status.getDescription(), isEmptyOrNullString());
    }

    @Test
    public void testConstructor3() throws Exception {
        Status status = new Status(OK);
        assertThat(status.getLevel(), is(OK));
        assertThat(status.getMessage(), isEmptyOrNullString());
        assertThat(status.getDescription(), isEmptyOrNullString());
    }
}
