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
package org.apache.dubbo.rpc.protocol.tri.h12.http2;

import org.apache.dubbo.common.stream.ServerCallStreamObserver;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http12.h2.Http2ServerChannelObserver;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ServerStreamObserver;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.compressor.Compressor;
import org.apache.dubbo.rpc.protocol.tri.h12.AttachmentHolder;
import org.apache.dubbo.rpc.protocol.tri.h12.CompressibleEncoder;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import java.util.Map;

public class Http2ServerStreamObserver extends Http2ServerChannelObserver
        implements ServerStreamObserver<Object>, ServerCallStreamObserver<Object>, AttachmentHolder {

    private final FrameworkModel frameworkModel;

    private Map<String, Object> attachments;

    public Http2ServerStreamObserver(FrameworkModel frameworkModel, H2StreamChannel h2StreamChannel) {
        super(h2StreamChannel);
        this.frameworkModel = frameworkModel;
    }

    @Override
    public void setCompression(String compression) {
        CompressibleEncoder compressibleEncoder = new CompressibleEncoder(getResponseEncoder());
        compressibleEncoder.setCompressor(Compressor.getCompressor(frameworkModel, compression));
        setResponseEncoder(compressibleEncoder);
    }

    @Override
    public void setResponseAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    @Override
    public Map<String, Object> getResponseAttachments() {
        return attachments;
    }

    @Override
    protected HttpMetadata encodeTrailers(Throwable throwable) {
        HttpMetadata metadata = super.encodeTrailers(throwable);
        HttpHeaders headers = metadata.headers();
        StreamUtils.putHeaders(headers, attachments, TripleProtocol.CONVERT_NO_LOWER_HEADER);
        return metadata;
    }
}
