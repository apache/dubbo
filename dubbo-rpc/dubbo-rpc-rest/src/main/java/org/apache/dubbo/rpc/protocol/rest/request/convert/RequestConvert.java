package org.apache.dubbo.rpc.protocol.rest.request.convert;

import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.client.ClientFacade;

public interface RequestConvert<REQ, RES> extends ClientFacade<REQ, RES> {

    REQ convert(RequestTemplate requestTemplate);

    Object convertResponse(RES response);

    public Object request(RequestTemplate requestTemplate) throws RemotingException;

}
