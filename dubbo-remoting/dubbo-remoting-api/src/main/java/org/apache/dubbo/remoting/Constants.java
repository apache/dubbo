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


public interface Constants {

    String BUFFER_KEY = "buffer";

    /**
     * default buffer size is 8k.
     */
    int DEFAULT_BUFFER_SIZE = 8 * 1024;

    int MAX_BUFFER_SIZE = 16 * 1024;

    int MIN_BUFFER_SIZE = 1 * 1024;

    String IDLE_TIMEOUT_KEY = "idle.timeout";

    int DEFAULT_IDLE_TIMEOUT = 600 * 1000;

    String ACCEPTS_KEY = "accepts";

    int DEFAULT_ACCEPTS = 0;

    String CONNECT_QUEUE_CAPACITY = "connect.queue.capacity";

    String CONNECT_QUEUE_WARNING_SIZE = "connect.queue.warning.size";

    int DEFAULT_CONNECT_QUEUE_WARNING_SIZE = 1000;

    String CHARSET_KEY = "charset";

    String DEFAULT_CHARSET = "UTF-8";

    /**
     * Every heartbeat duration / HEATBEAT_CHECK_TICK, check if a heartbeat should be sent. Every heartbeat timeout
     * duration / HEATBEAT_CHECK_TICK, check if a connection should be closed on server side, and if reconnect on
     * client side
     */
    int HEARTBEAT_CHECK_TICK = 3;

    /**
     * the least heartbeat during is 1000 ms.
     */
    long LEAST_HEARTBEAT_DURATION = 1000;

    /**
     * ticks per wheel.
     */
    int TICKS_PER_WHEEL = 128;
}
