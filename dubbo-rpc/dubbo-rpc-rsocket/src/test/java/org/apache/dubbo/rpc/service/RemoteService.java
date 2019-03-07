package org.apache.dubbo.rpc.service;

import java.rmi.RemoteException;

public interface RemoteService {
    String sayHello(String name) throws RemoteException;
}
