package com.alibaba.dubbo.rpc.benchmark;

/**
 * nfs-rpc
 *   Apache License
 *   
 *   http://code.google.com/p/nfs-rpc (c) 2011
 */
import java.io.Serializable;

/**
 * Just for RPC Benchmark Test,request object
 * 
 * @author <a href="mailto:bluedavy@gmail.com">bluedavy</a>
 */
public class RequestObject implements Serializable {

    private static final long serialVersionUID = 1L;

    public RequestObject(){
    }

    private byte[] bytes = null;

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public RequestObject(int size){
        bytes = new byte[size];
    }

    public byte[] getBytes() {
        return bytes;
    }

}
