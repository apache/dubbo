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
package org.apache.dubbo.rpc.cluster;

public interface Constants {
    String LOADBALANCE_KEY = "loadbalance";

    String DEFAULT_LOADBALANCE = "random";

    String FAIL_BACK_TASKS_KEY = "failbacktasks";

    int DEFAULT_FAILBACK_TASKS = 100;

    String RETRIES_KEY = "retries";

    int DEFAULT_RETRIES = 2;

    int DEFAULT_FAILBACK_TIMES = 3;

    String FORKS_KEY = "forks";

    int DEFAULT_FORKS = 2;

    String WEIGHT_KEY = "weight";

    int DEFAULT_WEIGHT = 100;

    String MOCK_PROTOCOL = "mock";

    String FORCE_KEY = "force";

    /**
     * To decide whether to exclude unavailable invoker from the cluster
     */
    String CLUSTER_AVAILABLE_CHECK_KEY = "cluster.availablecheck";

    /**
     * The default value of cluster.availablecheck
     *
     * @see #CLUSTER_AVAILABLE_CHECK_KEY
     */
    boolean DEFAULT_CLUSTER_AVAILABLE_CHECK = true;

    /**
     * To decide whether to enable sticky strategy for cluster
     */
    String CLUSTER_STICKY_KEY = "sticky";

    /**
     * The default value of sticky
     *
     * @see #CLUSTER_STICKY_KEY
     */
    boolean DEFAULT_CLUSTER_STICKY = false;

    String ADDRESS_KEY = "address";

    /**
     * When this attribute appears in invocation's attachment, mock invoker will be used
     */
    String INVOCATION_NEED_MOCK = "invocation.need.mock";

    /**
     * when ROUTER_KEY's value is set to ROUTER_TYPE_CLEAR, RegistryDirectory will clean all current routers
     */
    String ROUTER_TYPE_CLEAR = "clean";

    String DEFAULT_SCRIPT_TYPE_KEY = "javascript";

    String PRIORITY_KEY = "priority";

    String RULE_KEY = "rule";

    String TYPE_KEY = "type";

    String RUNTIME_KEY = "runtime";

    String REMOTE_TIMESTAMP_KEY = "remote.timestamp";

    String WARMUP_KEY = "warmup";

    int DEFAULT_WARMUP = 10 * 60 * 1000;

    String CONFIG_VERSION_KEY = "configVersion";

    String OVERRIDE_PROVIDERS_KEY = "providerAddresses";

    String TAG_KEY = "dubbo.tag";
    /**
     * key for router type, for e.g., "script"/"file",  corresponding to ScriptRouterFactory.NAME, FileRouterFactory.NAME
     */
    String ROUTER_KEY = "router";
    /**
     * The key name for reference URL in register center
     */
    String REFER_KEY = "refer";
    /**
     * The key name for export URL in register center
     */
    String EXPORT_KEY = "export";
}
