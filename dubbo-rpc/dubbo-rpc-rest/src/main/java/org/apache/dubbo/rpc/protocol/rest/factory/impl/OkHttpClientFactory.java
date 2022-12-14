package org.apache.dubbo.rpc.protocol.rest.factory.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.rest.factory.AbstractHttpClientFactory;

@Activate(value = "okhttp")
public class OkHttpClientFactory extends AbstractHttpClientFactory {

    @Override
    protected void beforeCreated(URL url) {

    }

    @Override
    protected RestClient doCreateRestClient(URL url) throws RpcException {

        // TODO
        return null;
    }
}
