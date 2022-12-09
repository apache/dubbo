package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.PathUtil;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionConfig;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

import java.util.Arrays;
import java.util.List;

@Activate(RestConstant.PATH_INTERCEPT)
public class PathVariableIntercept implements HttpConnectionPreBuildIntercept {

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        RestMethodMetadata restMethodMetadata = connectionCreateContext.getRestMethodMetadata();
        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();

        List<ArgInfo> argInfos = restMethodMetadata.getArgInfos();
        List<Object> realArgs = connectionCreateContext.getMethodRealArgs();

        // path variable parse
        String path = PathUtil.resolvePathVariable(restMethodMetadata.getRequest().getPath(), argInfos, Arrays.asList(realArgs));
        requestTemplate.path(path);


    }


}
