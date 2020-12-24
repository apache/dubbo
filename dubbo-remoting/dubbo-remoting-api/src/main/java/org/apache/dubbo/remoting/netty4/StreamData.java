package org.apache.dubbo.remoting.netty4;

import io.netty.buffer.ByteBuf;

public class StreamData extends BaseStreamState implements StreamState{

    private final ByteBuf data;

    public StreamData(boolean endOfStream, int id,ByteBuf data) {
        super(endOfStream, id);
        this.data=data;
    }


    ByteBuf data(){
        return data;
    }

}
