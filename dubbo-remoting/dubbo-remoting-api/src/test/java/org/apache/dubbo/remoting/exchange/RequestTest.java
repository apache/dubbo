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

class RequestTest {

    @Test
    void test() {
        Request request = new Request();
        request.setTwoWay(true);
        request.setBroken(true);
        request.setVersion("1.0.0");
        request.setEvent(true);
        request.setData("data");
        request.setPayload(1024);

        Assertions.assertTrue(request.isTwoWay());
        Assertions.assertTrue(request.isBroken());
        Assertions.assertTrue(request.isEvent());
        Assertions.assertEquals(request.getVersion(), "1.0.0");
        Assertions.assertEquals(request.getData(), "data");
        Assertions.assertTrue(request.getId() >= 0);
        Assertions.assertEquals(1024, request.getPayload());

        request.setHeartbeat(true);
        Assertions.assertTrue(request.isHeartbeat());

        Request copiedRequest = request.copy();
        Assertions.assertEquals(copiedRequest.toString(), request.toString());

        Request copyWithoutData = request.copyWithoutData();
        Assertions.assertNull(copyWithoutData.getData());
    }

}
