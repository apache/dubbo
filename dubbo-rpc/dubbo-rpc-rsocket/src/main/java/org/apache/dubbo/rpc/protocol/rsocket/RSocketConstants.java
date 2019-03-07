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
package org.apache.dubbo.rpc.protocol.rsocket;

/**
 * @author sixie.xyn on 2019/1/3.
 */
public class RSocketConstants {

    public static final String SERVICE_NAME_KEY = "_service_name";
    public static final String SERVICE_VERSION_KEY = "_service_version";
    public static final String METHOD_NAME_KEY = "_method_name";
    public static final String PARAM_TYPE_KEY = "_param_type";
    public static final String SERIALIZE_TYPE_KEY = "_serialize_type";
    public static final String TIMEOUT_KEY = "_timeout";


    public static final int FLAG_ERROR = 0x01;
    public static final int FLAG_NULL_VALUE = 0x02;
    public static final int FLAG_HAS_ATTACHMENT = 0x04;
}
