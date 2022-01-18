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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.remoting.exchange.Response;
import org.apache.dubbo.remoting.exchange.support.DefaultFuture2;
import org.apache.dubbo.rpc.AppResponse;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.tri.Compressor;
import org.apache.dubbo.rpc.protocol.tri.GrpcDataDecoder;
import org.apache.dubbo.rpc.protocol.tri.GrpcStatus;
import org.apache.dubbo.rpc.protocol.tri.Metadata;
import org.apache.dubbo.rpc.protocol.tri.TransportObserver;
import org.apache.dubbo.rpc.protocol.tri.TripleCommandOutBoundHandler;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.TripleHttp2ClientResponseHandler;
import org.apache.dubbo.rpc.protocol.tri.WriteQueue;
import org.apache.dubbo.rpc.protocol.tri.command.DataQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.command.HeaderQueueCommand;
import org.apache.dubbo.rpc.protocol.tri.frame.TriDecoder;
import org.apache.dubbo.rpc.protocol.tri.pack.Pack;
import org.apache.dubbo.rpc.protocol.tri.pack.Unpack;

import com.google.rpc.Status;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.Future;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.rpc.protocol.tri.Compressor.DEFAULT_COMPRESSOR;

public class ClientStream extends AbstractStream implements Stream {
    private final Compressor compressor;
    private final DefaultHttp2Headers headers;
    private final long id;
    private final WriteQueue writeQueue;
    private final Pack requestPack;
    private final Unpack responseUnpack;
    public final Listener responseListener;

    public ClientStream(long id,
                        Channel parent,
                        AsciiString scheme,
                        String path,
                        String serviceVersion,
                        String serviceGroup,
                        String application,
                        String authority,
                        String encoding,
                        String acceptEncoding,
                        String timeout,
                        Compressor compressor,
                        Map<String, Object> attachments,
                        Pack requestPack,
                        Unpack responseUnpack,
                        Listener listener) {
        this.id = id;
        this.compressor = compressor;
        this.writeQueue = createWriteQueue(parent);
        this.requestPack = requestPack;
        this.responseUnpack= responseUnpack;
        this.responseListener=listener;
        this.headers = new DefaultHttp2Headers(false);
        this.headers.scheme(scheme)
            .authority(authority)
            .method(HttpMethod.POST.asciiName())
            .path(path)
            .set(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader(), TripleConstant.CONTENT_PROTO)
            .set(HttpHeaderNames.TE, HttpHeaderValues.TRAILERS);
        setIfNotNull(headers, TripleHeaderEnum.TIMEOUT.getHeader(), timeout);
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_VERSION.getHeader(), serviceVersion);
        setIfNotNull(headers, TripleHeaderEnum.SERVICE_GROUP.getHeader(), serviceGroup);
        setIfNotNull(headers, TripleHeaderEnum.CONSUMER_APP_NAME_KEY.getHeader(), application);
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ENCODING.getHeader(), encoding);
        setIfNotNull(headers, TripleHeaderEnum.GRPC_ACCEPT_ENCODING.getHeader(), acceptEncoding);
        if (attachments != null) {
            convertAttachment(headers, attachments);
        }
    }

    private void setIfNotNull(DefaultHttp2Headers headers, CharSequence key, CharSequence value) {
        if (key == null) {
            return;
        }
        headers.set(key, value);
    }

    WriteQueue createWriteQueue(Channel parent) {
        final Http2StreamChannelBootstrap streamChannelBootstrap = new Http2StreamChannelBootstrap(parent);
        final Future<Http2StreamChannel> future = streamChannelBootstrap.open().syncUninterruptibly();
        if (!future.isSuccess()) {
            DefaultFuture2.getFuture(id()).cancel();
            return null;
        }
        final Http2StreamChannel channel = future.getNow();
        channel.pipeline()
            .addLast(new TripleCommandOutBoundHandler())
            .addLast(new TripleHttp2ClientResponseHandler())
            .addLast(new GrpcDataDecoder(Integer.MAX_VALUE, true))
            .addLast(new TripleClientInboundHandler());
        DefaultFuture2.addTimeoutListener(id(), channel::close);
        return new WriteQueue(channel);
    }

    public void startCall() {
        if (this.writeQueue == null) {
            // already processed at createStream()
            return;
        }
        final HeaderQueueCommand headerCmd = HeaderQueueCommand.createHeaders(headers);
        this.writeQueue.enqueue(headerCmd, false);
    }

    public void sendMessage(Object message) throws IOException {
        final byte[] data = requestPack.pack(message);

        final byte[] compress = compressor.compress(data);

        final DataQueueCommand dataCmd = DataQueueCommand.createGrpcCommand(compress, false);
        this.writeQueue.enqueue(dataCmd, false);
    }

    public void cancelByLocal(Throwable t){
        RpcContext.getCancellationContext().cancel(t);
    }
    public void cancelByRemote(){
        DefaultFuture2.getFuture(id()).cancel();
    }
    @Override
    public long id() {
        return id;
    }
    public final TransportObserver remoteObserver;

    public class ClientTransportObserver implements TransportObserver{

        private GrpcStatus transportError;
        private Compressor decompressor;
        private TriDecoder decoder;
        private Object appResponse;
        private Http2Headers headers;
        private Http2Headers trailers;
        private boolean terminated;
        private boolean headerReceived;

        private GrpcStatus validateHeaderStatus(Http2Headers headers){
            Integer httpStatus = headers.getInt(TripleHeaderEnum.);

            String contentType = headers.get(GrpcUtil.CONTENT_TYPE_KEY);
            if (!GrpcUtil.isGrpcContentType(contentType)) {
                return GrpcUtil.httpStatusToGrpcStatus(httpStatus)
                    .augmentDescription("invalid content-type: " + contentType);
            }
            return null;
        }
        @Override
        public void onHeader(Http2Headers headers, boolean endStream) {
            if(transportError!=null){
                transportError.appendDescription("headers:"+headers);
                return;
            }
            if(headerReceived){
                transportError=GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL)
                    .withDescription("Received headers twice");
                return;
            }
            Integer httpStatus=headers.status()==null?null:Integer.parseInt(headers.status().toString());

            if(httpStatus!=null&&Integer.parseInt(httpStatus.toString())>100&&httpStatus<200){
                // ignored
                return;
            }
            if (httpStatus == null) {
                transportError = GrpcStatus.fromCode(GrpcStatus.Code.INTERNAL).withDescription("Missing HTTP status code");
                return ;
            }
            final CharSequence contentType = headers.get(TripleHeaderEnum.CONTENT_TYPE_KEY.getHeader());
            if(contentType==null||!contentType.toString().startsWith(TripleHeaderEnum.APPLICATION_GRPC.getHeader())){
                transportError=GrpcStatus.fromCode(GrpcStatus.httpStatusToGrpcCode(httpStatus))
                    .withDescription("invalid content-type: "+contentType);
                return;
            }
            CharSequence messageEncoding = headers.get(TripleHeaderEnum.GRPC_ENCODING.getHeader());
            if (null != messageEncoding) {
                String compressorStr = messageEncoding.toString();
                if (!DEFAULT_COMPRESSOR.equals(compressorStr)) {
                    Compressor compressor = Compressor.getCompressor(url().getOrDefaultFrameworkModel(), compressorStr);
                    if (null == compressor) {
                        throw GrpcStatus.fromCode(GrpcStatus.Code.UNIMPLEMENTED)
                            .withDescription(String.format("Grpc-encoding '%s' is not supported", compressorStr))
                            .asException();
                    } else {
                        decompressor=compressor;
                    }
                }
            }

            AppResponse result=new AppResponse(appResponse);
            Response response = new Response(id(), TripleConstant.TRI_VERSION);
            result.setObjectAttachments(parseMetadataToAttachmentMap(trailers));
            response.setResult(result);
            responseListener.onResponse(response);
            TriDecoder.Listener listener=new TriDecoder.Listener() {
                @Override
                public void onRawMessage(byte[] data) {
                    appResponse = responseUnpack.unpack(data);

                }

                /**
                 * Parse metadata to a KV pairs map.
                 *
                 * @param trailers the metadata from remote
                 * @return KV pairs map
                 */
                private Map<String, Object> parseMetadataToAttachmentMap(Http2Headers trailers) {
                    Map<String, Object> attachments = new HashMap<>();
                    for (Map.Entry<CharSequence, CharSequence> header : trailers) {
                        String key = header.getKey().toString();
                        if (Http2Headers.PseudoHeaderName.isPseudoHeader(key)) {
                            continue;
                        }
                        // avoid subsequent parse protocol header
                        if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                            continue;
                        }
                        if (key.endsWith(TripleConstant.GRPC_BIN_SUFFIX) && key.length() > TripleConstant.GRPC_BIN_SUFFIX.length()) {
                            try {
                                attachments.put(key.substring(0, key.length() - TripleConstant.GRPC_BIN_SUFFIX.length()), decodeASCIIByte(header.getValue()));
                            } catch (Exception e) {
                                LOGGER.error("Failed to parse response attachment key=" + key, e);
                            }
                        } else {
                            attachments.put(key, header.getValue().toString());
                        }
                    }
                    return attachments;
                }
            };

        }

        @Override
        public void onData(ByteBuf data, boolean endStream) {
            decoder.onData(data);
            if(endStream) {
                decoder.close();
            }
        }

        @Override
        public void onError(GrpcStatus status) {
            // handle cancel

        }

        @Override
        public void onComplete() {

        }
    }
}