package org.apache.dubbo.remoting.netty4;

public abstract class BaseStreamState implements StreamState {
    private final boolean endOfStream;
    private final int id;

    protected BaseStreamState(boolean endOfStream, int id) {
        this.endOfStream = endOfStream;
        this.id = id;
    }

    public int id() {
        return id;
    }

    public boolean endOfStream() {
        return endOfStream;
    }
}
