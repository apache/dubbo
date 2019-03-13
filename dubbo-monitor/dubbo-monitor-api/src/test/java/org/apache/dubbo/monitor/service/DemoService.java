package org.apache.dubbo.monitor.service;

public interface DemoService {

    String sayName(String name);

    void timeoutException();

    void throwDemoException() throws Exception;

    int echo(int i);


}
