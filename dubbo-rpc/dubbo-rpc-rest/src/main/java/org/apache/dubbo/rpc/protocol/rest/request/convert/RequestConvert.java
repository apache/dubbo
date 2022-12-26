package org.apache.dubbo.rpc.protocol.rest.request.convert;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.client.ClientFacade;

@SPI
public interface RequestConvert<REQ, RES> extends ClientFacade<REQ, RES> {

    @Adaptive
    RequestConvert createRequestConvert(URL url, RestClient restClient, RestMethodMetadata restMethodMetadata);

    REQ convert(RequestTemplate requestTemplate);

    Object convertResponse(RES response) throws Exception;

    public Object request(RequestTemplate requestTemplate) throws RemotingException;

}
