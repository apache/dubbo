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
package org.apache.dubbo.registry.consul;

/**
 * @author cvictory ON 2019-08-02
 */
public class AbstractConsulRegistry {

    static final String SERVICE_TAG = "dubbo";
    static final String URL_META_KEY = "url";
    static final String WATCH_TIMEOUT = "consul-watch-timeout";
    static final String CHECK_PASS_INTERVAL = "consul-check-pass-interval";
    static final String DEREGISTER_AFTER = "consul-deregister-critical-service-after";

    static final int DEFAULT_PORT = 8500;
    // default watch timeout in millisecond
    static final int DEFAULT_WATCH_TIMEOUT = 60 * 1000;
    // default time-to-live in millisecond
    static final long DEFAULT_CHECK_PASS_INTERVAL = 16000L;
    // default deregister critical server after
    static final String DEFAULT_DEREGISTER_TIME = "20s";


}
