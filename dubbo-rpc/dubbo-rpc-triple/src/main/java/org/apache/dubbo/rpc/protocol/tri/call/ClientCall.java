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

package org.apache.dubbo.rpc.protocol.tri.call;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.remoting.api.Connection;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.compressor.Identity;
import org.apache.dubbo.rpc.protocol.tri.pack.PbPack;
import org.apache.dubbo.rpc.protocol.tri.pack.PbUnpack;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStream;
import org.apache.dubbo.rpc.protocol.tri.stream.ClientStreamListener;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.util.AsciiString;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class ClientCall {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientCall.class);
    private final Connection connection;
    private final Executor executor;
    private final DefaultHttp2Headers headers;
    private final URL url;
    private final PbUnpack<?> unpack;
    private Compressor compressor;
    private ClientStream stream;
    private boolean canceled;
    private boolean started;

    public ClientCall(URL url,
                      Connection connection,
                      AsciiString scheme,
                      String service,
                      String serviceVersion,
                      String serviceGroup,
                      String application,
                      String authority,
                      String timeout,
                      String methodName,
                      String acceptEncoding,
                      Compressor compressor,
                      Map<String, Object> attachments,
                      ExecutorService executor,
                      MethodDescriptor methodDescriptor
    ) {
        this.url = url;
        this.executor = new SerializingExecutor(executor);
        this.connection = connection;
        this.compressor = compressor;
        this.headers = new DefaultHttp2Headers(false);
        this.headers.scheme(scheme)
            .authority(authority)
            .method(HttpMethod.POST.asciiName())
            .path("/" + service + "/" + methodName)
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        setIfNotNull(headers, TripleHeaderEnum.TIMEOUT.getHeader(), timeout);
        if(!"1.0.0".equals(serviceVersion)) {
            setIfNotNull(headers, TripleHeaderEnum.SERVICE_VERSION.getHeader(), serviceVersion);
        }
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_GROUP.getHeader(), serviceGroup);
        setIfNotNull(headers, TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(), application);
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), acceptEncoding);
        StreamUtils.convertAttachment(headers, attachments);
        unpack = getUnpack(methodDescriptor);
    }

    private PbUnpack<?> getUnpack(MethodDescriptor methodDescriptor) {
        if (methodDescriptor.isNeedWrap()) {
            return PbUnpack.RESP_PB_UNPACK;
        }
        return new PbUnpack<>(methodDescriptor.getReturnClass());
    }

    private void setIfNotNull(DefaultHttp2Headers headers, CharSequence key, CharSequence value) {
        if (value == null) {
            return;
        }
        headers.set(key, value);
    }

    public void sendMessage(Object message) {
        if(!started) {
            started=true;
            setIfNotNull(headers, TripleHeaderEnum.GRPC_ENCODING.getHeader(), compressor.getMessageEncoding());
            stream.startCall(headers);
        }
        final byte[] data;
        try {
            data = PbPack.INSTANCE.pack(message);
            int compressed =  Identity.MESSAGE_ENCODING.equals(compressor.getMessageEncoding())?0:1;
            final byte[] compress = compressor.compress(data);
            stream.writeMessage(compress,compressed);
        } catch (IOException e) {
            cancel("Serialize request failed", e);
        }
    }

    public void halfClose() {
        stream.halfClose();
    }

    public void setCompression(String compression) {
        this.compressor = Compressor.getCompressor(url.getOrDefaultFrameworkModel(), compression);
    }

    public void start(Listener responseListener) {
        this.stream = new ClientStream(
            url,
            connection.getChannel(),
            new ClientStreamListenerImpl(responseListener, unpack));
    }

    public void cancel(String message, Throwable t) {
        if (canceled) {
            return;
        }
        canceled = true;
        if (stream != null) {
            final GrpcStatus status = GrpcStatus.fromCode(GrpcStatus.Code.CANCELLED);
            if (message != null) {
                status.withDescription(message);
            } else {
                status.withDescription("Cancel by client without message");
            }
            status.withCause(t);
            stream.cancelByLocal(status);
        }

    }

    interface Listener {

        void onMessage(Object message);

        void onClose(GrpcStatus status, Map<String, Object> trailers);
    }

    class ClientStreamListenerImpl implements ClientStreamListener {
        private final Listener listener;
        private final PbUnpack<?> unpack;
        private boolean done;

        ClientStreamListenerImpl(Listener listener, PbUnpack<?> unpack) {
            this.unpack = unpack;
            this.listener = listener;
        }

        @Override
        public void onMessage(byte[] message) {
            executor.execute(() -> {
                try {
                    final Object unpacked = unpack.unpack(message);
                    listener.onMessage(unpacked);
                } catch (IOException e) {
                    cancelByErr(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Deserialize response failed")
                        .withCause(e));
                }
            });
        }

        @Override
        public void complete(GrpcStatus grpcStatus, Map<String, Object> attachments) {
            executor.execute(() -> {
                if (done) {
                    return;
                }
                done = true;
                try {
                    listener.onClose(grpcStatus, attachments);
                } catch (Throwable t) {
                    cancelByErr(GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                        .withDescription("Close stream error")
                        .withCause(t));
                }
            });
        }

        void cancelByErr(GrpcStatus status) {
            stream.cancelByLocal(status);
        }
    }
}
