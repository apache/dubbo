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
package org.apache.dubbo.registry.multicast;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;

public class MulticastCommunication {
    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(MulticastCommunication.class);
    private final InetAddress multicastAddress;
    private final MulticastSocket multicastSocket;
    private final int multicastPort;

    public MulticastCommunication(InetAddress multicastAddress, MulticastSocket multicastSocket, int multicastPort) {
        this.multicastAddress = multicastAddress;
        this.multicastSocket = multicastSocket;
        this.multicastPort = multicastPort;
    }

    public void multicast(String msg) {
        if (logger.isInfoEnabled()) {
            logger.info("Send multicast message: " + msg + " to " + multicastAddress + ":" + multicastPort);
        }
        try {
            byte[] data = (msg + "\n").getBytes();
            DatagramPacket hi = new DatagramPacket(data, data.length, multicastAddress, multicastPort);
            multicastSocket.send(hi);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void unicast(String msg, String host) {
        if (logger.isInfoEnabled()) {
            logger.info("Send unicast message: " + msg + " to " + host + ":" + multicastPort);
        }
        try {
            byte[] data = (msg + "\n").getBytes();
            DatagramPacket hi = new DatagramPacket(data, data.length, InetAddress.getByName(host), multicastPort);
            multicastSocket.send(hi);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public void receive(String msg, InetSocketAddress remoteAddress, MulticastRegistry registry) {
        if (logger.isInfoEnabled()) {
            logger.info("Receive multicast message: " + msg + " from " + remoteAddress);
        }
        MulticastMessageHandler handler = MulticastMessageHandlerFactory.getHandler(msg);
        if (handler != null) {
            handler.handle(msg, registry);
        }
    }

    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }
}
