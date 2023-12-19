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
package org.apache.dubbo.rpc.protocol.tri.h12.grpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoderFactory;
import org.apache.dubbo.remoting.http12.message.HttpMessageEncoderFactory;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.remoting.utils.UrlUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

@Activate
public class GrpcCompositeCodecFactory implements HttpMessageEncoderFactory, HttpMessageDecoderFactory {

    private static final MediaType MEDIA_TYPE = new MediaType("application", "grpc");

    @Override
    public HttpMessageCodec createCodec(URL url, FrameworkModel frameworkModel, String mediaType) {
        final String serializeName = UrlUtils.serializationOrDefault(url);
        WrapperHttpMessageCodec wrapperHttpMessageCodec = new WrapperHttpMessageCodec(url, frameworkModel);
        wrapperHttpMessageCodec.setSerializeType(serializeName);
        ProtobufHttpMessageCodec protobufHttpMessageCodec = new ProtobufHttpMessageCodec();
        return new GrpcCompositeCodec(protobufHttpMessageCodec, wrapperHttpMessageCodec);
    }

    @Override
    public MediaType mediaType() {
        return MEDIA_TYPE;
    }
}
