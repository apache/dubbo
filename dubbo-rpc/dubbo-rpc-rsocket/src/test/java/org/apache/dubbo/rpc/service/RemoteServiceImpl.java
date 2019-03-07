package org.apache.dubbo.rpc.service;

import java.rmi.RemoteException;

public class RemoteServiceImpl implements RemoteService {
    @Override
    public String sayHello(String name) throws RemoteException {
        return "hello "+name;
    }
}
