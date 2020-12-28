package org.apache.dubbo.rpc.protocol.tri;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2HeadersEncoder;
import io.netty.handler.codec.http2.Http2Settings;

public class DubboConnectionHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<TripleHttp2ConnectionHandler, DubboConnectionHandlerBuilder> {
    @Override
    public DubboConnectionHandlerBuilder validateHeaders(boolean validateHeaders) {
        return super.validateHeaders(validateHeaders);
    }

    @Override
    public DubboConnectionHandlerBuilder initialSettings(Http2Settings settings) {
        return super.initialSettings(settings);
    }

    @Override
    public DubboConnectionHandlerBuilder frameListener(Http2FrameListener frameListener) {
        return super.frameListener(frameListener);
    }

    @Override
    public DubboConnectionHandlerBuilder gracefulShutdownTimeoutMillis(long gracefulShutdownTimeoutMillis) {
        return super.gracefulShutdownTimeoutMillis(gracefulShutdownTimeoutMillis);
    }

    @Override
    public DubboConnectionHandlerBuilder server(boolean isServer) {
        return super.server(isServer);
    }

    @Override
    public DubboConnectionHandlerBuilder connection(Http2Connection connection) {
        return super.connection(connection);
    }

    @Override
    public DubboConnectionHandlerBuilder maxReservedStreams(int maxReservedStreams) {
        return super.maxReservedStreams(maxReservedStreams);
    }

    @Override
    public DubboConnectionHandlerBuilder codec(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder) {
        return super.codec(decoder, encoder);
    }

    @Override
    public DubboConnectionHandlerBuilder frameLogger(Http2FrameLogger frameLogger) {
        return super.frameLogger(frameLogger);
    }

    @Override
    public DubboConnectionHandlerBuilder encoderEnforceMaxConcurrentStreams(
            boolean encoderEnforceMaxConcurrentStreams) {
        return super.encoderEnforceMaxConcurrentStreams(encoderEnforceMaxConcurrentStreams);
    }

    @Override
    public DubboConnectionHandlerBuilder encoderIgnoreMaxHeaderListSize(boolean encoderIgnoreMaxHeaderListSize) {
        return super.encoderIgnoreMaxHeaderListSize(encoderIgnoreMaxHeaderListSize);
    }

    @Override
    public DubboConnectionHandlerBuilder headerSensitivityDetector(Http2HeadersEncoder.SensitivityDetector headerSensitivityDetector) {
        return super.headerSensitivityDetector(headerSensitivityDetector);
    }

    @Override
    public DubboConnectionHandlerBuilder initialHuffmanDecodeCapacity(int initialHuffmanDecodeCapacity) {
        return super.initialHuffmanDecodeCapacity(initialHuffmanDecodeCapacity);
    }

    @Override
    public TripleHttp2ConnectionHandler build() {
        return super.build();
    }

    @Override
    protected TripleHttp2ConnectionHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                                 Http2Settings initialSettings) {
        return new TripleHttp2ConnectionHandler(decoder, encoder, initialSettings);
    }
}
