package org.apache.dubbo.remoting.netty4;

public interface StreamState {

    int id();

    boolean endOfStream();
}
