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
package org.apache.dubbo.remoting;

import org.apache.dubbo.common.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

<<<<<<< HEAD
public class TransportersTest {
=======
class TransportersTest {
>>>>>>> origin/3.2
    private String url = "dubbo://127.0.0.1:12345?transporter=mockTransporter";
    private ChannelHandler channel = Mockito.mock(ChannelHandler.class);

    @Test
<<<<<<< HEAD
    public void testBind() throws RemotingException {
=======
    void testBind() throws RemotingException {
>>>>>>> origin/3.2
        Assertions.assertThrows(RuntimeException.class, () -> Transporters.bind((String) null));
        Assertions.assertThrows(RuntimeException.class, () -> Transporters.bind((URL) null));
        Assertions.assertThrows(RuntimeException.class, () -> Transporters.bind(url));
        Assertions.assertNotNull(Transporters.bind(url, channel));
        Assertions.assertNotNull(Transporters.bind(url, channel, channel));
    }

    @Test
<<<<<<< HEAD
    public void testConnect() throws RemotingException {
=======
    void testConnect() throws RemotingException {
>>>>>>> origin/3.2
        Assertions.assertThrows(RuntimeException.class, () -> Transporters.connect((String) null));
        Assertions.assertThrows(RuntimeException.class, () -> Transporters.connect((URL) null));
        Assertions.assertNotNull(Transporters.connect(url));
        Assertions.assertNotNull(Transporters.connect(url, channel));
        Assertions.assertNotNull(Transporters.connect(url, channel, channel));
    }
}
