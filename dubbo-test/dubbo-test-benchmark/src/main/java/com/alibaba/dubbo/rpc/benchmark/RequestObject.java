package com.alibaba.dubbo.rpc.benchmark;


import java.io.Serializable;

public class RequestObject implements Serializable {

    private static final long serialVersionUID = 1L;
    private byte[] bytes = null;

    public RequestObject() {
    }

    public RequestObject(int size) {
        bytes = new byte[size];
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

}
