package org.apache.dubbo.remoting.http3.h3;

import org.apache.dubbo.remoting.http12.h2.Http2TransportListener;

public interface Http3TransportListener extends Http2TransportListener {
    void onDataCompletion();
}
