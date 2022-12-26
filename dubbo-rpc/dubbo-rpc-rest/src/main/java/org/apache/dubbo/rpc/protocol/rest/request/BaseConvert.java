package org.apache.dubbo.rpc.protocol.rest.request;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.convert.RequestConvert;


import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;


public abstract class BaseConvert<REQ, RES,CLIENT> implements RequestConvert<REQ, RES,CLIENT> {

    protected CLIENT restClient;
    protected RestMethodMetadata restMethodMetadata;
    protected URL url;

    public BaseConvert() {
    }

    public BaseConvert(CLIENT restClient, RestMethodMetadata restMethodMetadata, URL url) {
        this.restClient = restClient;
        this.restMethodMetadata = restMethodMetadata;
        this.url = url;
    }

    public Object request(RequestTemplate requestTemplate) throws RemotingException {


        REQ request = null;
        try {
            request = convert(requestTemplate);
        } catch (Exception e) {
            // TODO convert exception
        }


        RES response = null;
        try {
            response = send(request);
        } catch (RemotingException e) {
            // TODO send exception
        }

        Object result = null;
        try {
            result = convertResponse(response);
        } catch (Exception e) {
            // TODO response parse exception
        }

        return result;


    }



    protected Class<?> getReturnType() {
        Class<?> returnType = restMethodMetadata.getReflectMethod().getReturnType();
        return returnType;
    }


    protected int getTimeout() {
        int timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        return timeout;
    }


}
