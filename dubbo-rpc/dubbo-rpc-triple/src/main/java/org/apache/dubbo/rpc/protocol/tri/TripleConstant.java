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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.constants.CommonConstants;

import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;

public interface TripleConstant {
    String CONTENT_PROTO = "application/grpc+proto";
    String APPLICATION_GRPC = "application/grpc";
    String TRI_VERSION = "1.0.0";

    String SERIALIZATION_KEY = "serialization";
    String TE_KEY = "te";

    AttributeKey<Boolean> SSL_ATTRIBUTE_KEY = AttributeKey.valueOf(CommonConstants.SSL_ENABLED_KEY);


    AsciiString HTTPS_SCHEME = AsciiString.of("https");
    AsciiString HTTP_SCHEME = AsciiString.of("http");


    AttributeKey<AbstractServerStream> SERVER_STREAM_KEY = AttributeKey.newInstance(
        "tri_server_stream");
    AttributeKey<AbstractClientStream> CLIENT_STREAM_KEY = AttributeKey.newInstance(
        "tri_client_stream");

}
