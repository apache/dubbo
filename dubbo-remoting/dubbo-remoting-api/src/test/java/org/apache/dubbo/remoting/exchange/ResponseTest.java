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
package org.apache.dubbo.remoting.exchange;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.CommonConstants.HEARTBEAT_EVENT;

public class ResponseTest {
    @Test
    public void test() {
        Response response = new Response();
        response.setStatus(Response.OK);
        response.setId(1);
        response.setVersion("1.0.0");
        response.setResult("test");
        response.setEvent(HEARTBEAT_EVENT);
        response.setErrorMessage("errorMsg");

        Assertions.assertTrue(response.isEvent());
        Assertions.assertTrue(response.isHeartbeat());
        Assertions.assertEquals(response.getVersion(), "1.0.0");
        Assertions.assertEquals(response.getId(), 1);
        Assertions.assertEquals(response.getResult(), HEARTBEAT_EVENT);
        Assertions.assertEquals(response.getErrorMessage(), "errorMsg");
    }

}
