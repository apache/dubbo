package org.apache.dubbo.rpc.protocol.tri;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.stream.StreamObserver;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.rpc.model.MethodDescriptor;
import org.apache.dubbo.triple.TripleWrapper;

public class ServerInboundObserver implements StreamObserver<Object> {

    private StreamObserver<Object> respObserver;
    private MethodDescriptor md;
    private URL url;
    private String serializeType;
    private MultipleSerialization multipleSerialization;

    public ServerInboundObserver(StreamObserver<Object> respObserver, MethodDescriptor md) {
        this.respObserver = respObserver;
        if (md.isNeedWrap()) {
            final String value = url.getParameter(Constants.MULTI_SERIALIZATION_KEY, "default");
            this.multipleSerialization = ExtensionLoader.getExtensionLoader(MultipleSerialization.class).getExtension(
                value);
        }
    }

    @Override
    public void onNext(Object object) throws Exception {
        InputStream in = (InputStream)object;
        // TODO do not support multiple arguments for stream
        final Object[] resp = decodeRequestMessage(in);
        if (resp.length >= 1) {
            return;
        }
        respObserver.onNext(resp);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

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
}
