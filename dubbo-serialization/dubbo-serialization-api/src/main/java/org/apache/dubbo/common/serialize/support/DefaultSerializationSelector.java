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
package org.apache.dubbo.common.serialize.support;


public class DefaultSerializationSelector {

    private final static String DEFAULT_REMOTING_SERIALIZATION_PROPERTY_KEY = "DUBBO_DEFAULT_SERIALIZATION";

    private final static String DEFAULT_REMOTING_SERIALIZATION_PROPERTY = "hessian2";

    private final static String DEFAULT_REMOTING_SERIALIZATION;

    static {
        String fromProperty = System.getProperty(DEFAULT_REMOTING_SERIALIZATION_PROPERTY_KEY);
        if (fromProperty != null) {
            DEFAULT_REMOTING_SERIALIZATION = fromProperty;
        } else {
            String fromEnv = System.getenv(DEFAULT_REMOTING_SERIALIZATION_PROPERTY_KEY);
            if (fromEnv != null) {
                DEFAULT_REMOTING_SERIALIZATION = fromEnv;
            } else {
                DEFAULT_REMOTING_SERIALIZATION = DEFAULT_REMOTING_SERIALIZATION_PROPERTY;
            }
        }
    }

    public static String getDefaultRemotingSerialization() {
        return DEFAULT_REMOTING_SERIALIZATION;
    }
}
