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
package org.apache.dubbo.remoting.http12.h2;

import org.apache.dubbo.common.stream.ServerCallStreamObserver;
import org.apache.dubbo.remoting.http12.AbstractServerHttpChannelObserver;
import org.apache.dubbo.remoting.http12.ErrorCodeHolder;
import org.apache.dubbo.remoting.http12.FlowControlStreamObserver;
import org.apache.dubbo.remoting.http12.HttpConstants;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.remoting.http12.netty4.NettyHttpHeaders;
import org.apache.dubbo.rpc.CancellationContext;

import io.netty.handler.codec.http2.DefaultHttp2Headers;

/**
 * HTTP/2 server-side stream observer with flow control and backpressure support.
 * Implements {@link ServerCallStreamObserver} following gRPC's pattern for backpressure.
 */
public class Http2ServerChannelObserver extends AbstractServerHttpChannelObserver<H2StreamChannel>
        implements FlowControlStreamObserver<Object>,
                Http2CancelableStreamObserver<Object>,
                ServerCallStreamObserver<Object> {

    private CancellationContext cancellationContext;

    private StreamingDecoder streamingDecoder;

    private boolean autoRequestN = true;

    private Runnable onReadyHandler;

    public Http2ServerChannelObserver(H2StreamChannel h2StreamChannel) {
        super(h2StreamChannel);
    }

    /**
     * Returns whether the stream is ready for writing.
     * If false, the caller should avoid calling onNext to prevent blocking or excessive buffering.
     */
    public boolean isReady() {
        return getHttpChannel().isReady();
    }

    /**
     * Sets a callback to be invoked when the stream becomes ready for writing.
     */
    public void setOnReadyHandler(Runnable onReadyHandler) {
        this.onReadyHandler = onReadyHandler;
    }

    /**
     * Called when the channel writability changes.
     * Triggers the onReadyHandler if the channel is now writable.
     */
    public void onWritabilityChanged() {
        Runnable handler = this.onReadyHandler;
        if (handler != null && isReady()) {
            handler.run();
        }
    }

    public void setStreamingDecoder(StreamingDecoder streamingDecoder) {
        this.streamingDecoder = streamingDecoder;
    }

    @Override
    protected HttpMetadata encodeHttpMetadata(boolean endStream) {
        HttpHeaders headers = new NettyHttpHeaders<>(new DefaultHttp2Headers(false, 8));
        headers.set(HttpHeaderNames.TE.getKey(), HttpConstants.TRAILERS);
        return new Http2MetadataFrame(headers, endStream);
    }

    @Override
    protected HttpMetadata encodeTrailers(Throwable throwable) {
        return new Http2MetadataFrame(new NettyHttpHeaders<>(new DefaultHttp2Headers(false, 4)), true);
    }

    @Override
    public void setCancellationContext(CancellationContext cancellationContext) {
        this.cancellationContext = cancellationContext;
    }

    @Override
    public CancellationContext getCancellationContext() {
        return cancellationContext;
    }

    @Override
    public void cancel(Throwable throwable) {
        if (throwable instanceof CancelStreamException) {
            if (((CancelStreamException) throwable).isCancelByRemote()) {
                closed();
            }
        }
        if (cancellationContext != null) {
            cancellationContext.cancel(throwable);
        }
        long errorCode = 0;
        if (throwable instanceof ErrorCodeHolder) {
            errorCode = ((ErrorCodeHolder) throwable).getErrorCode();
        }
        getHttpChannel().writeResetFrame(errorCode);
    }

    @Override
    public void request(int count) {
        streamingDecoder.request(count);
    }

    @Override
    public void setCompression(String compression) {
        // not supported yet
    }

    @Override
    public void disableAutoFlowControl() {
        autoRequestN = false;
    }

    @Override
    public boolean isAutoRequestN() {
        return autoRequestN;
    }

    @Override
    public void close() {
        super.close();
        streamingDecoder.onStreamClosed();
    }
}
