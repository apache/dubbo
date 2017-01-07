package com.alibaba.dubbo.rpc.protocol.avro;

import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.HandshakeRequest;
import org.apache.avro.ipc.Responder;
import org.apache.avro.ipc.Transceiver;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.util.ByteBufferInputStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wuyu on 2016/6/14.
 */
public class MultiplexedResponder extends SpecificResponder {

    Map<String, Responder> responderMap = new ConcurrentHashMap<>();

    public MultiplexedResponder(Class iface, Object impl) {
        super(iface, impl);
        responderMap.put(new String(this.getLocal().getMD5()), new SpecificResponder(iface, impl));
    }

    @Override
    public List<ByteBuffer> respond(List<ByteBuffer> buffers, Transceiver connection) throws IOException {
        Responder responder = ResponderHolder.getResponder();
        if (responder == null) {
            List<ByteBuffer> newBuffers = new ArrayList<>();

            for (ByteBuffer bb : buffers) {
                ByteBuffer duplicate = bb.duplicate();
                newBuffers.add(duplicate);
            }


            Decoder in = DecoderFactory.get().binaryDecoder(new ByteBufferInputStream(newBuffers), null);
            SpecificDatumReader<HandshakeRequest> handshakeReader = new SpecificDatumReader<HandshakeRequest>(HandshakeRequest.class);
            HandshakeRequest request = handshakeReader.read(null, in);
            responder = this.responderMap.get(new String(request.getClientHash().bytes()));
            if (responder == null) {
                throw new IOException("Service name not found !");
            }
            ResponderHolder.setResponder(responder);
        }
        return responder.respond(buffers, connection);
    }

    public void registerResponder(Responder responder) {
        String clientHash = new String(responder.getLocal().getMD5());
        responderMap.put(clientHash, responder);
    }

    public void unRegisterResponder(Responder responder) {
        String clientHash = new String(responder.getLocal().getMD5());
        responderMap.remove(clientHash);
    }
}
