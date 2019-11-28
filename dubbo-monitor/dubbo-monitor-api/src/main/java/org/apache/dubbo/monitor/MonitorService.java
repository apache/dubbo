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


import org.apache.dubbo.common.URL;

import java.util.List;

import static org.apache.dubbo.rpc.Constants.INPUT_KEY;
import static org.apache.dubbo.rpc.Constants.OUTPUT_KEY;
/**
 * MonitorService. (SPI, Prototype, ThreadSafe)
 */
public interface MonitorService {

    String APPLICATION = "application";

    String INTERFACE = "interface";

    String METHOD = "method";

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

    /**
     * Collect monitor data
     * 1. support invocation count: count://host/interface?application=foo&method=foo&provider=10.20.153.11:20880&success=12&failure=2&elapsed=135423423
     * 1.1 host,application,interface,group,version,method: record source host/application/interface/method
     * 1.2 add provider address parameter if it's data sent from consumer, otherwise, add source consumer's address in parameters
     * 1.3 success,failure,elapsed: record success count, failure count, and total cost for success invocations, average cost (total cost/success calls)
     *
     * @param statistics
     */
    void collect(URL statistics);

    /**
     * Lookup monitor data
     * 1. support lookup by day: count://host/interface?application=foo&method=foo&side=provider&view=chart&date=2012-07-03
     * 1.1 host,application,interface,group,version,method: query criteria for looking up by host, application, interface, method. When one criterion is not present, it means ALL will be accepted, but 0.0.0.0 is ALL for host
     * 1.2 side=consumer,provider: decide the data from which side, both provider and consumer are returned by default
     * 1.3 default value is view=summary, to return the summarized data for the whole day. view=chart will return the URL address showing the whole day trend which is convenient for embedding in other web page
     * 1.4 date=2012-07-03: specify the date to collect the data, today is the default value
     *
     * @param query
     * @return statistics
     */
    List<URL> lookup(URL query);

}
