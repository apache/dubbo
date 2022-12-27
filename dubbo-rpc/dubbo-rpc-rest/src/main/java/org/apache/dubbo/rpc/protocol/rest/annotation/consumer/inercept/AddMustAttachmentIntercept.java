package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionConfig;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

@Activate(RestConstant.ADD_MUST_ATTTACHMENT)
public class AddMustAttachmentIntercept implements HttpConnectionPreBuildIntercept {

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();
        ServiceRestMetadata serviceRestMetadata = connectionCreateContext.getServiceRestMetadata();
        HttpConnectionConfig connectionConfig = connectionCreateContext.getConnectionConfig();
        MethodDefinition method = connectionCreateContext.getRestMethodMetadata().getMethod();


        //TODO  SERIALIZATION_KEY CONFIG
        requestTemplate.addHeader(RestConstant.GROUP, serviceRestMetadata.getGroup());
        requestTemplate.addHeader(RestConstant.VERSION, serviceRestMetadata.getVersion());
        requestTemplate.addHeader(RestConstant.METHOD, method.getName());
        requestTemplate.addHeader(RestConstant.PATH, serviceRestMetadata.getServiceInterface());
        requestTemplate.addKeepAliveHeader(connectionConfig.getKeepAlive());


    }


}
