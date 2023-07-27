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

import com.google.protobuf.Message;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProtobufHttpMessageCodec implements HttpMessageCodec {

    private static final MediaType MEDIA_TYPE = new MediaType("application", "x-protobuf");

    @Override
    public void encode(OutputStream outputStream, Object data) throws IOException {
        ((Message) data).writeTo(outputStream);
    }

    @Override
    public void encode(OutputStream outputStream, Object[] data) throws IOException {
        //
    }

    @Override
    public Object decode(InputStream body, Class<?> targetType) throws IOException {
        return SingleProtobufUtils.deserialize(body, targetType);
    }

    @Override
    public Object[] decode(InputStream body, Class<?>[] targetTypes) throws IOException {
        //only support one parameter
        return new Object[]{this.decode(body, targetTypes[0])};
    }

    @Override
    public MediaType contentType() {
        return MEDIA_TYPE;
    }
}
