package org.apache.dubbo.remoting.http3.h3;

public class Http3MetadataFrame {}

/*import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.h2.Http2Header;

public class Http3MetadataFrame implements Http2Header {
    @Override
    public HttpHeaders headers() {
        return null;
    }

    @Override
    public int id() {
        // todo: 怎么把quic stream id传给它
        return 0;
    }

    @Override
    public boolean isEndStream() {
        return false;
    }
}*/
