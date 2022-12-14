package org.apache.dubbo.rpc.protocol.rest.request.client;

import org.apache.dubbo.remoting.RemotingException;

public interface ClientFacade<REQ,RES> {

    RES send(REQ request) throws RemotingException;


}
