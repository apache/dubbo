package org.apache.dubbo.rpc.protocol.mvc.annotation.consumer.inercept;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.io.UnsafeByteArrayOutputStream;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.protocol.mvc.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.mvc.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.mvc.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;

import java.io.IOException;

@Activate(RestConstant.SERIALIZE_INTERCEPT)
public class SerializeBodyIntercept implements HttpConnectionPreBuildIntercept {
    private static final Logger logger = LoggerFactory.getLogger(SerializeBodyIntercept.class);

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {
        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();

        byte serializeId = requestTemplate.getSerializeId();
        Serialization serializationById = CodecSupport.getSerializationById((serializeId));
        Object unSerializedBody = requestTemplate.getUnSerializedBody();
        UnsafeByteArrayOutputStream os = new UnsafeByteArrayOutputStream(512);

        try {
            ObjectOutput serialize = serializationById.serialize(null, os);
            serialize.writeObject(unSerializedBody);
            requestTemplate.serializeBody(os.toByteArray());
        } catch (IOException e) {

            logger.error("MVC SerializeBodyIntercept serialize error: {}", e);
        }


    }


}
