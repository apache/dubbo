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

package org.apache.dubbo.rpc.protocol.tri;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransportStateTest {

    @Test
    void allowSendMeta() {
        TransportState transportState = new TransportState();
        transportState.setMetaSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        transportState.setResetSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        transportState.setMetaSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        Assertions.assertTrue(transportState.allowSendMeta());

        transportState = new TransportState();
        if (transportState.allowSendMeta()) {
            transportState.setMetaSend();
        }
        Assertions.assertFalse(transportState.allowSendMeta());
    }

    @Test
    void allowSendData() {
        TransportState transportState = new TransportState();
        Assertions.assertFalse(transportState.allowSendData());

        transportState = new TransportState();
        transportState.setResetSend();
        Assertions.assertFalse(transportState.allowSendData());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertFalse(transportState.allowSendData());

        transportState = new TransportState();
        transportState.setMetaSend();
        Assertions.assertTrue(transportState.allowSendData());
    }

    @Test
    void allowSendEndStream() {
        TransportState transportState = new TransportState();
        Assertions.assertTrue(transportState.allowSendEndStream());

        transportState = new TransportState();
        transportState.setResetSend();
        Assertions.assertFalse(transportState.allowSendEndStream());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertFalse(transportState.allowSendEndStream());

        transportState = new TransportState();
        transportState.setMetaSend();
        Assertions.assertTrue(transportState.allowSendEndStream());

    }

    @Test
    void allowSendReset() {
        TransportState transportState = new TransportState();
        transportState.setResetSend();
        Assertions.assertFalse(transportState.allowSendReset());

        transportState = new TransportState();
        Assertions.assertTrue(transportState.allowSendReset());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertTrue(transportState.allowSendReset());

        transportState = new TransportState();
        transportState.setMetaSend();
        Assertions.assertTrue(transportState.allowSendReset());
    }

    @Test
    void serverEndStream() {
        TransportState transportState = new TransportState();
        Assertions.assertFalse(transportState.serverSendStreamReceived());

        transportState.setServerEndStreamReceived();
        Assertions.assertTrue(transportState.serverSendStreamReceived());

    }
}