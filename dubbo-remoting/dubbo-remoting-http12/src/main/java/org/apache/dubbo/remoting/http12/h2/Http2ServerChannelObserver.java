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

import org.apache.dubbo.remoting.http12.AbstractServerHttpChannelObserver;
import org.apache.dubbo.remoting.http12.ErrorCodeHolder;
import org.apache.dubbo.remoting.http12.FlowControlStreamObserver;
import org.apache.dubbo.remoting.http12.HttpChannelObserver;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.message.StreamingDecoder;
import org.apache.dubbo.rpc.CancellationContext;

public class Http2ServerChannelObserver extends AbstractServerHttpChannelObserver
        implements HttpChannelObserver<Object>,
                FlowControlStreamObserver<Object>,
                Http2CancelableStreamObserver<Object> {

    private CancellationContext cancellationContext;

    private StreamingDecoder streamingDecoder;

    private boolean autoRequestN = true;

    public Http2ServerChannelObserver(H2StreamChannel h2StreamChannel) {
        super(h2StreamChannel);
    }

    public void setStreamingDecoder(StreamingDecoder streamingDecoder) {
        this.streamingDecoder = streamingDecoder;
    }

    @Override
    protected HttpMetadata encodeHttpMetadata() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaderNames.TE.getName(), "trailers");
        return new Http2MetadataFrame(httpHeaders);
    }

    @Override
    protected HttpMetadata encodeTrailers(Throwable throwable) {
        return new Http2MetadataFrame(new HttpHeaders(), true);
    }

    @Override
    public H2StreamChannel getHttpChannel() {
        return (H2StreamChannel) super.getHttpChannel();
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
        this.cancellationContext.cancel(throwable);
        long errorCode = 0;
        if (throwable instanceof ErrorCodeHolder) {
            errorCode = ((ErrorCodeHolder) throwable).getErrorCode();
        }
        getHttpChannel().writeResetFrame(errorCode);
    }

    @Override
    public void request(int count) {
        this.streamingDecoder.request(count);
    }

    @Override
    public void disableAutoFlowControl() {
        this.autoRequestN = false;
    }

    @Override
    public boolean isAutoRequestN() {
        return autoRequestN;
    }

    @Override
    public void close() throws Exception {
        super.close();
        streamingDecoder.onStreamClosed();
    }
}
