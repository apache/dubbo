package org.apache.dubbo.rpc.protocol.tri;

import com.google.protobuf.ByteString;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.serialize.MultipleSerialization;
import org.apache.dubbo.common.serialize.support.DefaultSerializationSelector;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.rpc.model.PackableMethod;
import org.apache.dubbo.triple.TripleWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;


public class TripleExceptionWrapperUtils {

    private final WrapExceptionResponsePack wrapExceptionResponsePack;
    private final WrapExceptionResponseUnpack wrapExceptionResponseUnPack;

    public WrapExceptionResponsePack getWrapExceptionResponsePack() {
        return wrapExceptionResponsePack;
    }

    public WrapExceptionResponseUnpack getWrapExceptionResponseUnPack() {
        return wrapExceptionResponseUnPack;
    }

    public byte[] packRequest(Object request) throws IOException {
        return getWrapExceptionResponsePack().pack(request);
    }

    public Object unPackRequest(byte[] bytes) throws IOException, ClassNotFoundException {
        return getWrapExceptionResponseUnPack().unpack(bytes);
    }

    public TripleExceptionWrapperUtils(Object object, URL url, String serializeName) {
        final MultipleSerialization serialization = url.getOrDefaultFrameworkModel()
            .getExtensionLoader(MultipleSerialization.class)
            .getExtension(url.getParameter(Constants.MULTI_SERIALIZATION_KEY,
                CommonConstants.DEFAULT_KEY));
        String returnType = null;
        if (object != null) {
            returnType = object.getClass().getName();
        }
        this.wrapExceptionResponsePack = new WrapExceptionResponsePack(serialization,
            url, returnType);
        this.wrapExceptionResponseUnPack = new WrapExceptionResponseUnpack(serialization, url);
    }

    public static TripleExceptionWrapperUtils init(Object object, URL url) {
        final String serializeName = url.getParameter(SERIALIZATION_KEY,
            DefaultSerializationSelector.getDefaultRemotingSerialization());
        TripleExceptionWrapperUtils tripleExceptionWrapperUtils = new TripleExceptionWrapperUtils(
            object, url, serializeName);
        return tripleExceptionWrapperUtils;
    }

    private static String convertHessianFromWrapper(String serializeType) {
        if (TripleConstant.HESSIAN4.equals(serializeType)) {
            return TripleConstant.HESSIAN2;
        }
        return serializeType;
    }

    private static class WrapExceptionResponsePack implements PackableMethod.Pack {

        private final MultipleSerialization multipleSerialization;
        private final URL url;
        private final String returnType;
        String serialize = "hessian4";

        private WrapExceptionResponsePack(MultipleSerialization multipleSerialization, URL url,
                                          String returnType) {
            this.multipleSerialization = multipleSerialization;
            this.url = url;
            this.returnType = returnType;
        }

        @Override
        public byte[] pack(Object obj) throws IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            multipleSerialization.serialize(url, serialize, null, obj, bos);
            return TripleWrapper.TripleExceptionWrapper.newBuilder()
                .setSerialization(serialize)
                .setClassName(obj.getClass().getSimpleName())
                .setData(ByteString.copyFrom(bos.toByteArray()))
                .build()
                .toByteArray();
        }
    }

    private static class WrapExceptionResponseUnpack implements PackableMethod.UnPack {

        private final MultipleSerialization serialization;
        private final URL url;

        private WrapExceptionResponseUnpack(MultipleSerialization serialization, URL url) {
            this.serialization = serialization;
            this.url = url;
        }

        @Override
        public Object unpack(byte[] data) throws IOException, ClassNotFoundException {

            TripleWrapper.TripleExceptionWrapper wrapper = TripleWrapper.TripleExceptionWrapper.parseFrom(
                data);
            final String serializeType = convertHessianFromWrapper(wrapper.getSerialization());
            ByteArrayInputStream bais = new ByteArrayInputStream(wrapper.getData().toByteArray());
            return serialization.deserialize(url, serializeType, wrapper.getClassName(), bais);
        }
    }

//    public static void main(String[] args) throws IOException, ClassNotFoundException {
//        ScoreException exception = new ScoreException();
//        URL url = URL.valueOf("www.baidu.com");
//        TripleExceptionWrapperUtils tripleExceptionWrapperUtils = TripleExceptionWrapperUtils.init(new ScoreException(), url);
//        byte[] tripleExceptionWrapperBytes = tripleExceptionWrapperUtils.packRequest(exception);
//        CharSequence msg = StreamUtils.encodeBase64ASCII(tripleExceptionWrapperBytes);
//        msg = TriRpcStatus.encodeMessage((String) msg);
//        msg = TriRpcStatus.decodeMessage(msg.toString());
//        byte[] tripleExceptionWrapperDecodeBytes = StreamUtils.decodeASCIIByte(msg);
////        TripleExceptionWrapperUtils tripleExceptionWrapperUtils2 = TripleExceptionWrapperUtils.init(new ScoreException(), url);
//        Object object1 = tripleExceptionWrapperUtils.unPackRequest(tripleExceptionWrapperDecodeBytes);
//        System.out.println(object1 instanceof ScoreException);
//        System.out.println(object1);
//    }
}
