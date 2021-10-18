package org.apache.dubbo.rpc.protocol.tri;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TransportStateTest {

    @Test
    void allowSendMeta() {
        TransportState transportState = new TransportState();
        transportState.setMetaSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        transportState.setResetSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        transportState.setMetaSend();
        Assertions.assertFalse(transportState.allowSendMeta());

        transportState = new TransportState();
        Assertions.assertTrue(transportState.allowSendMeta());
    }

    @Test
    void allowSendData() {
        TransportState transportState = new TransportState();
        Assertions.assertFalse(transportState.allowSendData());

        transportState = new TransportState();
        transportState.setResetSend();
        Assertions.assertFalse(transportState.allowSendData());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertFalse(transportState.allowSendData());

        transportState = new TransportState();
        transportState.setMetaSend();
        Assertions.assertTrue(transportState.allowSendData());
    }

    @Test
    void allowSendEndStream() {
        TransportState transportState = new TransportState();
        Assertions.assertFalse(transportState.allowSendEndStream());

        transportState = new TransportState();
        transportState.setResetSend();
        Assertions.assertFalse(transportState.allowSendEndStream());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertFalse(transportState.allowSendEndStream());

        transportState = new TransportState();
        transportState.setMetaSend();
        Assertions.assertTrue(transportState.allowSendEndStream());

    }

    @Test
    void allowSendReset() {
        TransportState transportState = new TransportState();
        transportState.setResetSend();
        Assertions.assertFalse(transportState.allowSendReset());

        transportState = new TransportState();
        Assertions.assertTrue(transportState.allowSendReset());

        transportState = new TransportState();
        transportState.setEndStreamSend();
        Assertions.assertTrue(transportState.allowSendReset());

        transportState = new TransportState();
        transportState.setMetaSend();
        Assertions.assertTrue(transportState.allowSendReset());
    }
}