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
package org.apache.dubbo.registry.sofa;

/**
 * @author <a href="mailto:ujjboy@qq.com">GengZhang</a>
 * @since 2.7.2
 */
public class SofaRegistryConstants {

    /**
     * Default data center
     */
    public static final String LOCAL_DATA_CENTER = "DefaultDataCenter";

    /**
     * Default region
     */
    public static final String LOCAL_REGION = "DEFAULT_ZONE";

    /**
     * Default group
     */
    public static final String DEFAULT_GROUP = "SOFA";

    /**
     * parameter for address.wait.time of rpc reference
     */
    public static final String ADDRESS_WAIT_TIME_KEY = "rpc.reference.address.wait.time";
}
