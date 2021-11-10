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
package org.apache.dubbo.monitor;

import static org.apache.dubbo.rpc.Constants.INPUT_KEY;
import static org.apache.dubbo.rpc.Constants.OUTPUT_KEY;

public interface Constants {
    String DUBBO_PROVIDER = "dubbo.provider";

    String DUBBO_CONSUMER = "dubbo.consumer";

    String DUBBO_PROVIDER_METHOD = "dubbo.provider.method";

    String DUBBO_CONSUMER_METHOD = "dubbo.consumer.method";

    String SERVICE = "service";

    String METHOD = "method";

    String DUBBO_GROUP = "dubbo";

    String METRICS_KEY = "metrics";

    String LOGSTAT_PROTOCOL = "logstat";

    String COUNT_PROTOCOL = "count";

    String MONITOR_SEND_DATA_INTERVAL_KEY = "interval";

    int DEFAULT_MONITOR_SEND_DATA_INTERVAL = 60000;

    String APPLICATION = "application";

    String INTERFACE = "interface";

    String GROUP = "group";

    String VERSION = "version";

    String CONSUMER = "consumer";

    String PROVIDER = "provider";

    String TIMESTAMP = "timestamp";

    String SUCCESS = "success";

    String FAILURE = "failure";

    String INPUT = INPUT_KEY;

    String OUTPUT = OUTPUT_KEY;

    String ELAPSED = "elapsed";

    String CONCURRENT = "concurrent";

    String MAX_INPUT = "max.input";

    String MAX_OUTPUT = "max.output";

    String MAX_ELAPSED = "max.elapsed";

    String MAX_CONCURRENT = "max.concurrent";
}
