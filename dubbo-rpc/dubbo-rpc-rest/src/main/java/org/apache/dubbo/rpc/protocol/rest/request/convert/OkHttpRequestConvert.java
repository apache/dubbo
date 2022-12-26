package org.apache.dubbo.rpc.protocol.rest.request.convert;

import okhttp3.*;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.http.RestClient;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.BaseConvert;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Adaptive("okhttp")
public class OkHttpRequestConvert extends BaseConvert<Request, Response> {

    public OkHttpRequestConvert(RestClient restClient, RestMethodMetadata restMethodMetadata, URL url) {
        super(restClient, restMethodMetadata, url);
    }

    public OkHttpRequestConvert() {
    }

    @Override
    public Response send(Request request) throws RemotingException {
        OkHttpClient client = new OkHttpClient();
        OkHttpClient clientWith60sTimeout = client.newBuilder().
            readTimeout(60, TimeUnit.SECONDS).
            build();

        try {
            return clientWith60sTimeout.newCall(request).execute();
        } catch (IOException e) {

        }

        return null;


    }


    @Override
    public RequestConvert createRequestConvert(URL url, RestClient restClient, RestMethodMetadata restMethodMetadata) {
        return new OkHttpRequestConvert(restClient, restMethodMetadata, url);
    }

    @Override
    public Request convert(RequestTemplate requestTemplate) {
        Request.Builder builder = new Request.Builder();
        // url
        builder.url(requestTemplate.getURL());

        Map<String, Collection<String>> allHeaders = requestTemplate.getAllHeaders();

        // header
        for (String headerName : allHeaders.keySet()) {
            Collection<String> headerValues = allHeaders.get(headerName);

            for (String headerValue : headerValues) {
                builder.addHeader(headerName, headerValue);
            }
        }
        builder.method(requestTemplate.getHttpMethod(), RequestBody.create(null, requestTemplate.getSerializedBody()));
        return builder.build();
    }

    @Override
    public Object convertResponse(Response response) throws Exception {
        ResponseBody body = response.body();
        int code = response.code();

        return JsonUtils.getJson().parseObject(body.byteStream(), getReturnType());
    }
}
