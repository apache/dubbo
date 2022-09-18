package org.apache.dubbo.rpc.protocol.tri.support;

public interface IGreeter2 {
    String SERVER_MSG = "HELLO WORLD";

    /**
     * Use request to respond
     */
    String echo(String request) throws IGreeterException;
}
