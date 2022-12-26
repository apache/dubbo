package org.apache.dubbo.rpc.protocol.rest.factory.impl;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.remoting.http.okhttp.OKHttpRestClient;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.rest.factory.AbstractHttpClientFactory;

@Activate("okhttp")
public class OkHttpClientFactory extends AbstractHttpClientFactory {


    @Override
    protected RestClient doCreateRestClient(URL url) throws RpcException {

        return new OKHttpRestClient(httpClientConfig);
    }
}
