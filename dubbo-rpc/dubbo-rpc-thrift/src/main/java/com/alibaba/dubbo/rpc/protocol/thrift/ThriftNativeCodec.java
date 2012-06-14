package com.alibaba.dubbo.rpc.protocol.thrift;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TMessageType;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.transport.TIOStreamTransport;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec;
import com.alibaba.dubbo.remoting.exchange.Request;
import com.alibaba.dubbo.remoting.exchange.Response;
import com.alibaba.dubbo.rpc.Invocation;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class ThriftNativeCodec implements Codec {
    
    private final AtomicInteger thriftSeq = new AtomicInteger(0);
    
    public void encode(Channel channel, OutputStream output, Object message)
        throws IOException {
        if (message instanceof Request) {
            encodeRequest(channel, output, (Request)message);
        } else if (message instanceof Response) {
            encodeResponse(channel, output, (Response)message);
        } else {
            throw new IOException("Unsupported message type "
                                      + message.getClass().getName());
        }
    }

    protected void encodeRequest(Channel channel, OutputStream output, Request request)
        throws IOException {
        Invocation invocation = (Invocation) request.getData();
        TProtocol protocol = newProtocol(channel.getUrl(), output);
        try {
            protocol.writeMessageBegin(new TMessage(
                invocation.getMethodName(), TMessageType.CALL, 
                thriftSeq.getAndIncrement()));
            protocol.writeStructBegin(new TStruct(invocation.getMethodName() + "_args"));
            for(int i = 0; i < invocation.getParameterTypes().length; i++) {
                Class<?> type = invocation.getParameterTypes()[i];

            }
        } catch (TException e) {
            throw new IOException(e.getMessage(), e);
        }

    }

    protected void encodeResponse(Channel channel, OutputStream output, Response response)
        throws IOException {

    }

    public Object decode(Channel channel, InputStream input) throws IOException {
        return null;
    }

    protected static TProtocol newProtocol(URL url, OutputStream output) throws IOException {
        String protocol = url.getParameter(ThriftConstants.THRIFT_PROTOCOL_KEY,
                                           ThriftConstants.DEFAULT_PROTOCOL);
        if (ThriftConstants.BINARY_THRIFT_PROTOCOL.equals(protocol)) {
            return new TBinaryProtocol(new TIOStreamTransport(output));
        }
        throw new IOException("Unsupported protocol type " + protocol);
    }

}
