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
package org.apache.dubbo.metrics.observation;

import io.micrometer.common.docs.KeyName;
import io.micrometer.common.lang.NonNullApi;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Documentation of Dubbo observations.
 */
public enum DubboObservationDocumentation implements ObservationDocumentation {

    /**
     * Server side Dubbo RPC Observation.
     */
    SERVER {
        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultDubboServerObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeyNames.values();
        }

    },

    /**
     * Client side Dubbo RPC Observation.
     */
    CLIENT {
        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultDubboClientObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return LowCardinalityKeyNames.values();
        }

    };

    @NonNullApi
    enum LowCardinalityKeyNames implements KeyName {

        /**
         * A string identifying the remoting system.
         * Must be "apache_dubbo".
         */
        RPC_SYSTEM {
            @Override
            public String asString() {
                return "rpc.system";
            }
        },

        /**
         * The full (logical) name of the service being called, including its package name, if applicable.
         * Example: "myservice.EchoService".
         */
        RPC_SERVICE {
            @Override
            public String asString() {
                return "rpc.service";
            }
        },

        /**
         * The name of the (logical) method being called, must be equal to the $method part in the span name.
         * Example: "exampleMethod".
         */
        RPC_METHOD {
            @Override
            public String asString() {
                return "rpc.method";
            }
        },

        /**
         * RPC server host name.
         * Example: "example.com".
         */
        NET_PEER_NAME {
            @Override
            public String asString() {
                return "net.peer.name";
            }
        },

        /**
         * Logical remote port number.
         * Example: 80; 8080; 443.
         */
        NET_PEER_PORT {
            @Override
            public String asString() {
                return "net.peer.port";
            }
        }
    }

}
