package org.apache.dubbo.rpc.protocol.tri.support;

public class IGreeter2Impl implements IGreeter2{
    @Override
    public String echo(String request)  throws IGreeterException {
        throw new IGreeterException("I am self define exception");
    }
}
