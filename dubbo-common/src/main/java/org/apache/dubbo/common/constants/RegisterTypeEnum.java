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

/**
 * Indicate that a service need to be registered to registry or not
 */
public enum RegisterTypeEnum {

    /**
     * Never register. Cannot be registered by any command(like QoS-online).
     */
    NEVER_REGISTER,

    /**
     * Manual register. Can be registered by command(like QoS-online), but not register by default.
     */
    MANUAL_REGISTER,

    /**
     * (INTERNAL) Auto register by deployer. Will be registered after deployer started.
     * (Delay publish when starting. Prevent service from being invoked before all services are started)
     */
    AUTO_REGISTER_BY_DEPLOYER,

    /**
     * Auto register. Will be registered when one service is exported.
     */
    AUTO_REGISTER;
}
