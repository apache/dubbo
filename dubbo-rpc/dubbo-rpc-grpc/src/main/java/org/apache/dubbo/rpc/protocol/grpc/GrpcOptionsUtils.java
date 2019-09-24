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
package org.apache.dubbo.rpc.protocol.grpc;

import org.apache.dubbo.common.URL;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * Support gRPC configs in the Dubbo specific way.
 */
public class GrpcOptionsUtils {

    static Server buildServerBuilder(URL url, ServerBuilder builder) {

        return builder.build();
    }

    static ManagedChannel buildManagedChannel(URL url) {
        return ManagedChannelBuilder.forAddress(url.getHost(), url.getPort()).usePlaintext(true).build();
    }

    static CallOptions buildCallOptions(URL url) {
        return CallOptions.DEFAULT;
    }
}
