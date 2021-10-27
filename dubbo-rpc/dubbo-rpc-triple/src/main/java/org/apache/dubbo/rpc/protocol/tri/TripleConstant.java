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

import io.netty.util.AsciiString;
import io.netty.util.AttributeKey;

public class TripleConstant {

    public static final String CONTENT_PROTO = "application/grpc+proto";
    public static final String APPLICATION_GRPC = "application/grpc";
    public static final String TEXT_PLAIN_UTF8 = "text/plain; encoding=utf-8";
    public static final String TRI_VERSION = "1.0.0";

    public static final String SERIALIZATION_KEY = "serialization";
    public static final String TE_KEY = "te";


    public static final String HESSIAN4 = "hessian4";
    public static final String HESSIAN2 = "hessian2";


    public static final String GRPC_BIN_SUFFIX = "-bin";

    public static final AsciiString HTTPS_SCHEME = AsciiString.of("https");
    public static final AsciiString HTTP_SCHEME = AsciiString.of("http");

    public static final AttributeKey<AbstractServerStream> SERVER_STREAM_KEY = AttributeKey.valueOf("tri_server_stream");
    public static final AttributeKey<AbstractClientStream> CLIENT_STREAM_KEY = AttributeKey.valueOf("tri_client_stream");

    public static final String SUCCESS_RESPONSE_MESSAGE = "OK";
    public static final String SUCCESS_RESPONSE_STATUS = Integer.toString(GrpcStatus.Code.OK.code);

    public static final Metadata SUCCESS_RESPONSE_META = getSuccessResponseMeta();

    static Metadata getSuccessResponseMeta() {
        Metadata metadata = new DefaultMetadata();
        metadata.put(TripleHeaderEnum.MESSAGE_KEY.getHeader(), TripleConstant.SUCCESS_RESPONSE_MESSAGE);
        metadata.put(TripleHeaderEnum.STATUS_KEY.getHeader(), TripleConstant.SUCCESS_RESPONSE_STATUS);
        return metadata;
    }

}
