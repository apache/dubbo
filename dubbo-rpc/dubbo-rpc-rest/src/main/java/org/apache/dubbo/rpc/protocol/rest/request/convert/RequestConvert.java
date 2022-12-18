package org.apache.dubbo.rpc.protocol.rest.request.convert;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.client.ClientFacade;

@SPI
public interface RequestConvert<REQ, RES> extends ClientFacade<REQ, RES> {

    @Adaptive({Constants.CLIENT_KEY})
    RequestConvert createRequestConvert(URL url);

    REQ convert(RequestTemplate requestTemplate);

    Object convertResponse(RES response);

    public Object request(RequestTemplate requestTemplate) throws RemotingException;

}
