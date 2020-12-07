package org.apache.dubbo.remoting.transport.netty4.invocation;

public interface StreamInboundListener {
    void onHeader(DataHeader header);

    int onBody(DataBody body);

    void onError(Throwable t);

    boolean isComplete();

}
