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
package org.apache.dubbo.remoting.telnet.support;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.telnet.support.command.ClearTelnetHandler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ClearTelnetHandlerTest {

    @Test
    public void test() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            buf.append("\r\n");
        }

        ClearTelnetHandler telnetHandler = new ClearTelnetHandler();
        Assertions.assertEquals(buf.toString(), telnetHandler.telnet(Mockito.mock(Channel.class), "50"));

        // Illegal Input
        Assertions.assertTrue(telnetHandler.telnet(Mockito.mock(Channel.class), "Illegal").contains("Illegal"));

        for (int i = 0; i < 50; i++) {
            buf.append("\r\n");
        }
        Assertions.assertEquals(buf.toString(), telnetHandler.telnet(Mockito.mock(Channel.class), ""));
    }
}
