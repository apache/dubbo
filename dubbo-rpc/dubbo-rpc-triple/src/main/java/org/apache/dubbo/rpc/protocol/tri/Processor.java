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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

// metadata + is -> object
// object -> is
public class Processor {
    private ServerStream serverStream;
    private MethodDescriptor md;
    private boolean needWrap;
    private String serializeType;
    private MultipleSerialization multipleSerialization;
    private URL url;

    public Processor(ServerStream serverStream) {
        this.serverStream = serverStream;
        this.url = url;
        this.needWrap = needWrap;
        if (needWrap) {
            loadFromURL(url);
        }
    }

    public void onSingleMessage(InputStream in) throws Exception {
        if (serverStream instanceof StreamServerStream) {
            StreamServerStream stream = (StreamServerStream)serverStream;
            final Object[] resp = decodeRequestMessage(in);
            if (resp.length >= 1) {
                return;
            }
            stream.getObserver().onNext(resp[0]);
        }

    }

    public Object[] decodeRequestMessage(InputStream is) {
        if (md.isNeedWrap()) {
            final TripleWrapper.TripleRequestWrapper req = TripleUtil.unpack(is, TripleWrapper.TripleRequestWrapper.class);
            this.serializeType = req.getSerializeType();
            String[] paramTypes = req.getArgTypesList().toArray(new String[req.getArgsCount()]);
            if (!Arrays.equals(this.md.getCompatibleParamSignatures(), paramTypes)) {
                //todo error
            }
            final Object[] arguments = TripleUtil.unwrapReq(url, req, multipleSerialization);
            return arguments;
        } else {
            final Object req = TripleUtil.unpack(is, md.getParameterClasses()[0]);
            return new Object[]{req};
        }
    }

    public ByteBuf encodeResponse(Object value, ChannelHandlerContext ctx) {
        final Message message;
        final ByteBuf buf;

            if (md.isNeedWrap()) {
                message = TripleUtil.wrapResp(url, serializeType, value, md, multipleSerialization);
            } else {
                message = (Message) value;
            }
            buf = TripleUtil.pack(ctx, message);
        return buf;
    }

    protected void loadFromURL(URL url) {
        final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, "default");
        this.multipleSerialization = ExtensionLoader.getExtensionLoader(MultipleSerialization.class).getExtension(value);
    }

    //class StreamOutboundWriter implements StreamObserver<Object> {
    //
    //    private StreamServerStream stream;
    //    private final AtomicBoolean canceled = new AtomicBoolean();
    //
    //    public StreamOutboundWriter(StreamServerStream stream) {
    //        this.stream = stream;
    //    }
    //
    //    @Override
    //    public void onNext(Object o) throws Exception {
    //
    //        stream.write(o, null);
    //    }
    //
    //    @Override
    //    public void onError(Throwable t) {
    //        doCancel();
    //    }
    //
    //    @Override
    //    public void onComplete() {
    //        stream.onComplete();
    //    }
    //
    //    public void doCancel() {
    //        if (canceled.compareAndSet(false, true)) {
    //            stream.onComplete();
    //        }
    //    }
    //}
}
