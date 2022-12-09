package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

@Activate(RestConstant.REQUEST_PARAM_INTERCEPT)
public class RequestParamIntercept implements HttpConnectionPreBuildIntercept {

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        RestMethodMetadata restMethodMetadata = connectionCreateContext.getRestMethodMetadata();
        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();


        // TODO add param according to arg info


    }


}
