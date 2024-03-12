package org.apache.dubbo.rpc.protocol.tri.h3;

import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMetadata;
import org.apache.dubbo.remoting.http12.h2.H2StreamChannel;
import org.apache.dubbo.remoting.http3.h3.Http3MetadataFrame;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.TripleProtocol;
import org.apache.dubbo.rpc.protocol.tri.h12.http2.Http2ServerCallToObserverAdapter;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

public class Http3ServerCallToObserverAdapter extends Http2ServerCallToObserverAdapter {

    public Http3ServerCallToObserverAdapter(FrameworkModel frameworkModel, H2StreamChannel h2StreamChannel) {
        super(frameworkModel, h2StreamChannel);
    }

    @Override
    protected HttpMetadata encodeHttpMetadata() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaderNames.TE.getName(), "trailers");
        return new Http3MetadataFrame(httpHeaders);
    }

    @Override
    protected HttpMetadata encodeTrailers(Throwable throwable) {
        HttpMetadata httpMetadata = new Http3MetadataFrame(new HttpHeaders());
        HttpHeaders headers = httpMetadata.headers();
        StreamUtils.putHeaders(headers, attachments, TripleProtocol.CONVERT_NO_LOWER_HEADER);
        return httpMetadata;
    }
}
