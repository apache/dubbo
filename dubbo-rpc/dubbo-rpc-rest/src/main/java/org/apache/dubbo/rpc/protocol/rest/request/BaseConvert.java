package org.apache.dubbo.rpc.protocol.rest.request;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.protocol.rest.ReferenceCountedClient;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.convert.RequestConvert;
import org.apache.dubbo.rpc.protocol.rest.util.NumberUtils;
import org.apache.dubbo.rpc.protocol.rest.util.TypeUtil;

import java.nio.charset.Charset;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;


public abstract class BaseConvert<REQ, RES> implements RequestConvert<REQ, RES> {

    protected RestClient restClient;
    protected RestMethodMetadata restMethodMetadata;
    protected URL url;

    protected BaseConvert(ReferenceCountedClient<? extends RestClient> referenceCountedClient,
                          RestMethodMetadata restMethodMetadata, URL url) {
        this.restClient = referenceCountedClient.getClient();
        this.restMethodMetadata = restMethodMetadata;
        this.url = url;
    }

    public Object request(RequestTemplate requestTemplate) throws RemotingException {


        REQ request = convert(requestTemplate);


        RES response = send(request);

        Object result = convertResponse(response);

        return result;


    }

    protected Object parseResponse(byte[] content, Charset charset) {
        Class returnType = restMethodMetadata.getReflectMethod().getReturnType();
        if (TypeUtil.isString(returnType)) {
            return new String(content, charset);
        }

        if (TypeUtil.isNumberType(returnType)) {
            return NumberUtils.parseNumber(new String(content, charset), returnType);
        }

        return JsonUtils.getJson().parseObject(content, returnType);
    }

    protected Object parseResponse(byte[] content) {
        return parseResponse(content, Charset.defaultCharset());
    }

    protected int getTimeout() {
        int timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        return timeout;
    }


}
