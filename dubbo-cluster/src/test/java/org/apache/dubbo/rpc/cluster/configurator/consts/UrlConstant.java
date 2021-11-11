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
package org.apache.dubbo.rpc.cluster.configurator.consts;

/**
 * test case url constant
 */
public class UrlConstant {
    public static final String URL_CONSUMER = "dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&side=consumer";
    public static final String URL_ONE = "dubbo://10.20.153.10:20880/com.foo.BarService?application=foo&timeout=1000&side=consumer";
    public static final String APPLICATION_BAR_SIDE_CONSUMER_11 = "dubbo://10.20.153.11:20880/com.foo.BarService?application=bar&side=consumer";
    public static final String TIMEOUT_1000_SIDE_CONSUMER_11 = "dubbo://10.20.153.11:20880/com.foo.BarService?application=bar&timeout=1000&side=consumer";
    public static final String SERVICE_TIMEOUT_200 = "override://10.20.153.10/com.foo.BarService?timeout=200";
    public static final String APPLICATION_BAR_SIDE_CONSUMER_10 = "dubbo://10.20.153.10:20880/com.foo.BarService?application=bar&side=consumer";
    public static final String TIMEOUT_1000_SIDE_CONSUMER_10 = "dubbo://10.20.153.10:20880/com.foo.BarService?application=bar&timeout=1000&side=consumer";

}
