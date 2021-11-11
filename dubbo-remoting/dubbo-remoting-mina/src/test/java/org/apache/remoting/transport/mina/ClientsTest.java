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
package org.apache.remoting.transport.mina;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.remoting.Transporter;
import org.apache.dubbo.remoting.transport.mina.MinaTransporter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

public class ClientsTest {

    @Test
    public void testGetTransportEmpty() {
        try {
            ExtensionLoader.getExtensionLoader(Transporter.class).getExtension("");
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    public void testGetTransportNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            String name = null;
            ExtensionLoader.getExtensionLoader(Transporter.class).getExtension(name);
        });
    }

    @Test
    public void testGetTransport1() {
        String name = "mina";
        assertEquals(MinaTransporter.class, ExtensionLoader.getExtensionLoader(Transporter.class).getExtension(name).getClass());
    }

    @Test
    public void testGetTransportWrong() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            String name = "nety";
            assertNull(ExtensionLoader.getExtensionLoader(Transporter.class).getExtension(name).getClass());
        });
    }
}
