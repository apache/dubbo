package org.apache.dubbo.rpc.protocol.tri;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

// metadata + is -> object
// object -> is
public class Processor {
    private final ServerStream serverStream;
    private final MethodDescriptor md;
    private final MultipleSerialization multipleSerialization;
    private final URL url;
    private String serializeType;

    public Processor(ServerStream serverStream, MethodDescriptor md, URL url, String serializeType,
        MultipleSerialization multipleSerialization) {
        this.serverStream = serverStream;
        this.md = md;
        this.url = url;
        this.serializeType = serializeType;
        this.multipleSerialization = multipleSerialization;
    }

    public void onSingleMessage(InputStream in) throws Exception {
        if (serverStream instanceof StreamServerStream) {
            StreamServerStream stream = (StreamServerStream)serverStream;
            final Object[] resp = decodeRequestMessage(in);
            if (resp.length > 1) {
                return;
            }
            stream.getObserver().onNext(resp[0]);
        }

    }

    public Object[] decodeRequestMessage(InputStream is) {
        if (md.isNeedWrap()) {
            final TripleWrapper.TripleRequestWrapper req = TripleUtil.unpack(is,
                TripleWrapper.TripleRequestWrapper.class);
            this.serializeType = req.getSerializeType();
            String[] paramTypes = req.getArgTypesList().toArray(new String[req.getArgsCount()]);
            if (!Arrays.equals(this.md.getCompatibleParamSignatures(), paramTypes)) {
                //todo error
            }
            final Object[] arguments = TripleUtil.unwrapReq(url, req, multipleSerialization);
            return arguments;
        } else {
            final Object req = TripleUtil.unpack(is, md.getParameterClasses()[0]);
            return new Object[] {req};
        }
    }

    public ByteBuf encodeResponse(Object value, ChannelHandlerContext ctx) {
        final Message message;
        final ByteBuf buf;

        if (md.isNeedWrap()) {
            message = TripleUtil.wrapResp(url, serializeType, value, md, multipleSerialization);
        } else {
            message = (Message)value;
        }
        buf = TripleUtil.pack(ctx, message);
        return buf;
    }
}
