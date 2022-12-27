package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

import java.io.ByteArrayOutputStream;

@Activate(RestConstant.SERIALIZE_INTERCEPT)
public class SerializeBodyIntercept implements HttpConnectionPreBuildIntercept {
    private static final Logger logger = LoggerFactory.getLogger(SerializeBodyIntercept.class);

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {
        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();

        if (requestTemplate.isBodyEmpty()) {
            return;
        }
        Object unSerializedBody = requestTemplate.getUnSerializedBody();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JsonUtils.getJson().serializeObject(outputStream, unSerializedBody);
            requestTemplate.serializeBody(outputStream.toByteArray());
        } catch (Exception e) {

            logger.error("MVC SerializeBodyIntercept serialize error: {}", e);
        }


    }


}
