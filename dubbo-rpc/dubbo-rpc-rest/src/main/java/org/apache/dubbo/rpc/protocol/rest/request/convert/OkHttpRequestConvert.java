package org.apache.dubbo.rpc.protocol.rest.request.convert;

import okhttp3.*;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.http.okhttp.OKHttpRestClient;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.BaseConvert;

import java.util.Collection;
import java.util.Map;

@Adaptive("okhttp")
public class OkHttpRequestConvert extends BaseConvert<Request, Response, OKHttpRestClient> {

    public OkHttpRequestConvert(OKHttpRestClient restClient, RestMethodMetadata restMethodMetadata, URL url) {
        super(restClient, restMethodMetadata, url);
    }

    public OkHttpRequestConvert() {
    }

    @Override
    public Response send(Request request) throws Exception {
        return getRestClient().send(request);
    }


    @Override
    public RequestConvert createRequestConvert(URL url, OKHttpRestClient restClient, RestMethodMetadata restMethodMetadata) {
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

        if (requestTemplate.emptyBody()) {
            return builder.method(requestTemplate.getHttpMethod(), null).build();
        }

        builder.method(requestTemplate.getHttpMethod(), RequestBody.create(null, requestTemplate.getSerializedBody()));
        return builder.build();
    }

    @Override
    public Object convertResponse(Response response) throws Exception {
        ResponseBody body = response.body();
        int code = response.code();

        // TODO judge code
        return JsonUtils.getJson().parseObject(body.byteStream(), getReturnType());
    }
}
