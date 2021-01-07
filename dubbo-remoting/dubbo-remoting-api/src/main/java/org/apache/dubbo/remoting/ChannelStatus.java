package org.apache.dubbo.remoting;

import java.util.concurrent.atomic.AtomicReference;

public class ChannelStatus {
    enum Status{
        UNCONNECTED,
        CONNECTED,
        READ_ONLY,
        DISCONNECTED,
        CLOSED
    }
    private AtomicReference<Status> status=new AtomicReference<>(Status.UNCONNECTED);

    public boolean isConnected(){
        return status.get().equals(Status.CONNECTED);
    }
    public boolean isClosed(){
        return status.get().equals(Status.CLOSED);
    }
    public void connected(){
        status.set(Status.CONNECTED);
    }
    public void close(){
        status.set(Status.CLOSED);
    }
}
