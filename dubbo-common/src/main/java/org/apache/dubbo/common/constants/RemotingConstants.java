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
public interface RemotingConstants {

    String PAYLOAD_KEY = "payload";
    /**
     * 8M
     */
    int DEFAULT_PAYLOAD = 8 * 1024 * 1024;

    String CONNECT_TIMEOUT_KEY = "connect.timeout";

    int DEFAULT_CONNECT_TIMEOUT = 3000;

    String HEARTBEAT_KEY = "heartbeat";

    int DEFAULT_HEARTBEAT = 60 * 1000;

    String SERIALIZATION_KEY = "serialization";

    String DEFAULT_REMOTING_SERIALIZATION = "hessian2";

    String CODEC_KEY = "codec";

    String SERVER_KEY = "server";

    String DEFAULT_REMOTING_SERVER = "netty";

    String CLIENT_KEY = "client";

    String DEFAULT_REMOTING_CLIENT = "netty";

    String TRANSPORTER_KEY = "transporter";

    String DEFAULT_TRANSPORTER = "netty";

    String EXCHANGER_KEY = "exchanger";

    String DEFAULT_EXCHANGER = "header";

    String DISPACTHER_KEY = "dispacther";

    int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    String BIND_IP_KEY = "bind.ip";

    String BIND_PORT_KEY = "bind.port";

    String SENT_KEY = "sent";

    String DISPATCHER_KEY = "dispatcher";

    String CHANNEL_ATTRIBUTE_READONLY_KEY = "channel.readonly";

    String CHANNEL_READONLYEVENT_SENT_KEY = "channel.readonly.sent";

    String CHANNEL_SEND_READONLYEVENT_KEY = "channel.readonly.send";

    String EXECUTOR_SERVICE_COMPONENT_KEY = ExecutorService.class.getName();

    String BACKUP_KEY = "backup";

    String HEARTBEAT_TIMEOUT_KEY = "heartbeat.timeout";

    String RECONNECT_KEY = "reconnect";

    int DEFAULT_RECONNECT_PERIOD = 2000;

    String SEND_RECONNECT_KEY = "send.reconnect";

    String CHECK_KEY = "check";

    String PROMPT_KEY = "prompt";

    String DEFAULT_PROMPT = "dubbo>";
}
