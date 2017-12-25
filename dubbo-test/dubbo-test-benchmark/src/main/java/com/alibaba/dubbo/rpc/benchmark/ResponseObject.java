package com.alibaba.dubbo.rpc.benchmark;

import java.io.Serializable;

public class ResponseObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] bytes = null;

    public ResponseObject(int size) {
        bytes = new byte[size];
    }

    public byte[] getBytes() {
        return bytes;
    }


}
