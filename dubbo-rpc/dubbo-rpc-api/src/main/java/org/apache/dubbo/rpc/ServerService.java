package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;

public interface ServerService {

    <T> Invoker<T> getInvoker(URL url);

}
