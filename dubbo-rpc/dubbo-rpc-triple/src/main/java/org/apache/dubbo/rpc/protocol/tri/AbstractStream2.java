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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.io.InputStream;

public abstract class AbstractStream2 implements Stream {
    private final URL url;
    private final MultipleSerialization multipleSerialization;
    private ServiceDescriptor serviceDescriptor;
    private MethodDescriptor methodDescriptor;
    private StreamObserver<Object> streamSubscriber;
    private StreamObserver<Object> streamObserver;
    private TransportObserver transportObserver;
    private TransportObserver transportSubscriber;
    protected AbstractStream2(URL url) {
        this.url = url;
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, CommonConstants.DEFAULT_KEY);
        this.multipleSerialization = ExtensionLoader.getExtensionLoader(MultipleSerialization.class)
                .getExtension(value);
    }

    public MultipleSerialization getMultipleSerialization() {
        return multipleSerialization;
    }

    public StreamObserver<Object> getStreamSubscriber() {
        return streamSubscriber;
    }

    public StreamObserver<Object> getStreamObserver() {
        return streamObserver;
    }

    public TransportObserver getTransportSubscriber() {
        return transportSubscriber;
    }

    public MethodDescriptor getMethodDescriptor() {
        return methodDescriptor;
    }

    public void setMethodDescriptor(MethodDescriptor methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public ServiceDescriptor getServiceDescriptor() {
        return serviceDescriptor;
    }

    public void setServiceDescriptor(ServiceDescriptor serviceDescriptor) {
        this.serviceDescriptor = serviceDescriptor;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public void subscribe(StreamObserver<Object> observer) {
        this.streamSubscriber = observer;
    }

    @Override
    public void subscribe(TransportObserver observer) {
        this.transportSubscriber = observer;
    }

    @Override
    public StreamObserver<Object> asStreamObserver() {
        return streamObserver;
    }

    @Override
    public TransportObserver asTransportObserver() {
        return transportObserver;
    }

    protected void transportError(GrpcStatus status) {
        Metadata metadata = new Metadata();
        metadata.put(TripleConstant.STATUS_KEY, Integer.toString(status.code.code));
        metadata.put(TripleConstant.MESSAGE_KEY, status.toMessage());
        getTransportSubscriber().onMetadata(metadata, true, null);
    }

    protected static abstract class AbstractTransportObserver implements TransportObserver {
        private Metadata headers;
        private Metadata trailers;

        public Metadata getHeaders() {
            return headers;
        }

        public Metadata getTrailers() {
            return trailers;
        }

        @Override
        public void onMetadata(Metadata metadata, boolean endStream, OperationHandler handler) {
            if (headers == null) {
                headers = metadata;
            } else {
                trailers = metadata;
            }
        }

    }

    protected abstract static class UnaryTransportObserver extends AbstractTransportObserver implements TransportObserver {
        private InputStream data;

        public InputStream getData() {
            return data;
        }

        @Override
        public void onData(InputStream in, boolean endStream, OperationHandler handler) {
            if (data == null) {
                this.data = in;
            } else {
                handler.operationDone(OperationResult.FAILURE, GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription(Stream.DUPLICATED_REQ).asException());
            }
        }
    }

}
