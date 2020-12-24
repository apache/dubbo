package org.apache.dubbo.remoting;

import org.apache.dubbo.remoting.exchange.Response;

public class Http2Packet {
    private int streamId;
    private Object inv;
    private Object result;
    private byte status;
    private String errorMessage;

    public Http2Packet(int streamId, Object inv, Object result) {
        this.streamId = streamId;
        this.inv = inv;
        this.result = result;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public Object getInv() {
        return inv;
    }

    public void setInv(Object inv) {
        this.inv = inv;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
