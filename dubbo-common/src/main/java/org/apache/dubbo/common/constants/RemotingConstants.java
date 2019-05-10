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

package org.apache.dubbo.common.constants;

import java.util.concurrent.ExecutorService;

/**
 * RemotingConstants
 */
public class RemotingConstants {

    public static final String PAYLOAD_KEY = "payload";
    /**
     * 8M
     */
    public static final int DEFAULT_PAYLOAD = 8 * 1024 * 1024;

    public static final String BUFFER_KEY = "buffer";

    /**
     * default buffer size is 8k.
     */
    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;

    public static final int MAX_BUFFER_SIZE = 16 * 1024;

    public static final int MIN_BUFFER_SIZE = 1 * 1024;

    public static final String CONNECT_TIMEOUT_KEY = "connect.timeout";

    public static final int DEFAULT_CONNECT_TIMEOUT = 3000;

    public static final String HEARTBEAT_KEY = "heartbeat";

    public static final int DEFAULT_HEARTBEAT = 60 * 1000;

    public static final String IDLE_TIMEOUT_KEY = "idle.timeout";

    public static final int DEFAULT_IDLE_TIMEOUT = 600 * 1000;

    public static final String ACCEPTS_KEY = "accepts";

    public static final int DEFAULT_ACCEPTS = 0;

    public static final String SERIALIZATION_KEY = "serialization";

    public static final String DEFAULT_REMOTING_SERIALIZATION = "hessian2";

    public static final String CODEC_KEY = "codec";

    public static final String DEFAULT_REMOTING_CODEC = "dubbo";

    public static final String SERVER_KEY = "server";

    public static final String DEFAULT_REMOTING_SERVER = "netty";

    public static final String CLIENT_KEY = "client";

    public static final String DEFAULT_REMOTING_CLIENT = "netty";

    public static final String TRANSPORTER_KEY = "transporter";

    public static final String DEFAULT_TRANSPORTER = "netty";

    public static final String EXCHANGER_KEY = "exchanger";

    public static final String DEFAULT_EXCHANGER = "header";

    public static final String DISPACTHER_KEY = "dispacther";

    public static final int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    public static final String BIND_IP_KEY = "bind.ip";

    public static final String BIND_PORT_KEY = "bind.port";

    public static final String SENT_KEY = "sent";

    public static final boolean DEFAULT_SENT = false;

    public static final String DISPATCHER_KEY = "dispatcher";

    public static final String CHANNEL_HANDLER_KEY = "channel.handler";

    public static final String DEFAULT_CHANNEL_HANDLER = "default";

    public static final String SERVICE_DESCIPTOR_KEY = "serviceDescriptor";

    public static final String CONNECT_QUEUE_CAPACITY = "connect.queue.capacity";

    public static final String CONNECT_QUEUE_WARNING_SIZE = "connect.queue.warning.size";

    public static final int DEFAULT_CONNECT_QUEUE_WARNING_SIZE = 1000;

    public static final String CHANNEL_ATTRIBUTE_READONLY_KEY = "channel.readonly";

    public static final String CHANNEL_READONLYEVENT_SENT_KEY = "channel.readonly.sent";

    public static final String CHANNEL_SEND_READONLYEVENT_KEY = "channel.readonly.send";

    public static final String EXECUTOR_SERVICE_COMPONENT_KEY = ExecutorService.class.getName();

    public static final String CHARSET_KEY = "charset";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String BACKUP_KEY = "backup";

    /**
     * Every heartbeat duration / HEATBEAT_CHECK_TICK, check if a heartbeat should be sent. Every heartbeat timeout
     * duration / HEATBEAT_CHECK_TICK, check if a connection should be closed on server side, and if reconnect on
     * client side
     */
    public static final int HEARTBEAT_CHECK_TICK = 3;

    /**
     * the least heartbeat during is 1000 ms.
     */
    public static final long LEAST_HEARTBEAT_DURATION = 1000;

    /**
     * ticks per wheel.
     */
    public static final int TICKS_PER_WHEEL = 128;

    public static final String HEARTBEAT_TIMEOUT_KEY = "heartbeat.timeout";

    public static final String RECONNECT_KEY = "reconnect";

    public static final int DEFAULT_RECONNECT_PERIOD = 2000;

    public static final String SEND_RECONNECT_KEY = "send.reconnect";

    public static final String CHECK_KEY = "check";

    public static final String PROMPT_KEY = "prompt";

    public static final String DEFAULT_PROMPT = "dubbo>";
}
