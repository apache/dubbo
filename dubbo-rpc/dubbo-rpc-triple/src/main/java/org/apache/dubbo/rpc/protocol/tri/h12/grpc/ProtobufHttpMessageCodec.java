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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.exception.EncodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;
import org.apache.dubbo.rpc.protocol.tri.SingleProtobufUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Activate(onClass = "com.google.protobuf.Message")
public class ProtobufHttpMessageCodec implements HttpMessageCodec {

    private static final MediaType MEDIA_TYPE = new MediaType("application", "x-protobuf");

    @Override
    public void encode(OutputStream outputStream, Object data) throws EncodeException {
        try {
            SingleProtobufUtils.serialize(data, outputStream);
        } catch (IOException e) {
            throw new EncodeException(e);
        }
    }

    @Override
    public Object decode(InputStream inputStream, Class<?> targetType) throws DecodeException {
        try {
            return SingleProtobufUtils.deserialize(inputStream, targetType);
        } catch (IOException e) {
            throw new DecodeException(e);
        }
    }

    @Override
    public MediaType contentType() {
        return MEDIA_TYPE;
    }
}
