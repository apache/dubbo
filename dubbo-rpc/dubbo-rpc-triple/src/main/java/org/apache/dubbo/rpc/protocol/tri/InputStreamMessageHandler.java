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

import org.apache.dubbo.remoting.http12.EmptyInputStreamMessage;
import org.apache.dubbo.remoting.http12.HttpInputMessage;
import org.apache.dubbo.remoting.http12.message.DefaultListeningDecoder;
import org.apache.dubbo.remoting.http12.message.DefaultStreamingDecoder;
import org.apache.dubbo.remoting.http12.message.HttpMessageDecoder;
import org.apache.dubbo.remoting.http12.message.ListeningDecoder;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamMessageHandler implements InputMessageHandler<InputStream> {

    @Override
    public StreamingDecoder<InputStream> createStreamingDecoder() {
        return new DefaultStreamingDecoder();
    }

    @Override
    public ListeningDecoder<InputStream> createListeningDecoder(
            HttpMessageDecoder httpMessageDecoder, Class<?>[] targetTypes) {
        return new DefaultListeningDecoder(httpMessageDecoder, targetTypes);
    }

    @Override
    public MethodDescriptor findMethodDescriptor(
            ServiceDescriptor serviceDescriptor, String methodName, InputStream rawMessage) throws IOException {
        return DescriptorUtils.findTripleMethodDescriptor(serviceDescriptor, methodName, rawMessage);
    }

    @Override
    public String supportProtocol() {
        return "http";
    }

    @Override
    public HttpInputMessage<InputStream> empty() {
        return EmptyInputStreamMessage.INSTANCE;
    }
}
