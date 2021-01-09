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
package org.apache.dubbo.registry.dns.util;

public class DNSClientConst {

    public final static String ADDRESS_PREFIX = "addressPrefix";

    public final static String ADDRESS_SUFFIX = "addressSuffix";

    public final static String MAX_QUERIES_PER_RESOLVE = "maxQueriesPerResolve";

    /**
     * To decide the frequency of execute DNS poll (in ms)
     */
    public final static String DNS_POLLING_CYCLE = "dnsPollingCycle";

    /**
     * Default value for check frequency: 60000 (ms)
     */
    public final static int DEFAULT_DNS_POLLING_CYCLE = 60000;

    /**
     * To decide how many threads used to execute DNS poll
     */
    public final static String DNS_POLLING_POOL_SIZE_KEY = "dnsPollingPoolSize";

    /**
     * Default value for DNS pool thread: 1
     */
    public final static int DEFAULT_DNS_POLLING_POOL_SIZE = 1;

}
