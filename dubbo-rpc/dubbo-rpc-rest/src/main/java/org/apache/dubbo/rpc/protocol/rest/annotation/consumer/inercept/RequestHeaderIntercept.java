package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

import java.util.Set;

@Activate(RestConstant.REQUEST_HEADER_INTERCEPT)
public class RequestHeaderIntercept implements HttpConnectionPreBuildIntercept {

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        RestMethodMetadata restMethodMetadata = connectionCreateContext.getRestMethodMetadata();
        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();



        Set<String> consumes = restMethodMetadata.getRequest().getConsumes();

        requestTemplate.addHeaders("Accept", consumes);

    }


}
