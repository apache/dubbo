package org.apache.dubbo.rpc.protocol.rest.request.client;


public interface ClientFacade<REQ,RES> {

    RES send(REQ request) throws Exception;


}
