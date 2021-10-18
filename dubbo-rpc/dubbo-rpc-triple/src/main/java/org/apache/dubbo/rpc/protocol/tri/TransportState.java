package org.apache.dubbo.rpc.protocol.tri;

public class TransportState {

    private volatile int state = 0;
    private static final int META_SEND = 0b00000000000000000000000000000001;
    private static final int RESET_SEND = 0b00000000000000000000000000000010;
    private static final int END_STREAM_SEND = 0b00000000000000000000000000000100;

    private static final int ALLOW_META_SEND = 0b00000000000000000000000000000000;
    private static final int ALLOW_DATA_SEND = META_SEND;
    private static final int ALLOW_END_STREAM_SEND = META_SEND;
    private static final int ALLOW_RESET_SEND = 0b00000000000000000000000000000001;

    public TransportState() {
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setMetaSend() {
        this.state = this.state | META_SEND;
    }

    public void setResetSend() {
        this.state = this.state | RESET_SEND;
    }

    public void setEndStreamSend() {
        this.state = this.state | END_STREAM_SEND;
    }

    public boolean allowSendMeta() {
        return this.state == ALLOW_META_SEND;
    }

    public boolean allowSendReset() {
        return (this.state & RESET_SEND) != RESET_SEND;
    }

    public boolean allowSendData() {
        return this.state == ALLOW_DATA_SEND;
    }

    public boolean allowSendEndStream() {
        return this.state == ALLOW_END_STREAM_SEND;
    }

}
