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
package org.apache.dubbo.rpc.protocol.tri.h3;

import org.apache.dubbo.remoting.http12.HttpConstants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;

import io.netty.incubator.codec.http3.DefaultHttp3Headers;

public final class Helper {

    private Helper() {}

    public static HttpMetadata encodeHttpMetadata(boolean endStream) {
        HttpHeaders headers = new NettyHttpHeaders<>(new DefaultHttp3Headers(false, 8));
        headers.set(HttpHeaderNames.TE.getKey(), HttpConstants.TRAILERS);
        return new Http2MetadataFrame(headers, endStream);
    }

    public static HttpMetadata encodeTrailers() {
        return new Http2MetadataFrame(new NettyHttpHeaders<>(new DefaultHttp3Headers(false, 4)), true);
    }
}
