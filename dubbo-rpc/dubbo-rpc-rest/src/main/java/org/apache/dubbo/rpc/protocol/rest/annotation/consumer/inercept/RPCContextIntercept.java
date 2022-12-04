package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.protocol.mvc.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.mvc.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.mvc.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.mvc.constans.RestConstant;

import java.nio.charset.StandardCharsets;
import java.util.Map;
@Activate(RestConstant.RPCCONTEXT_INTERCEPT)
public class RPCContextIntercept implements HttpConnectionPreBuildIntercept {


    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();
        int size = 0;
        for (Map.Entry<String, Object> entry : RpcContext.getClientAttachment().getObjectAttachments().entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            if (illegalHttpHeaderKey(key) || illegalHttpHeaderValue(value)) {
                throw new IllegalArgumentException("The attachments of " + RpcContext.class.getSimpleName() + " must not contain ',' or '=' when using rest protocol");
            }

            // TODO for now we don't consider the differences of encoding and server limit
            if (value != null) {
                size += value.getBytes(StandardCharsets.UTF_8).length;
            }
            if (size > RestConstant.MAX_HEADER_SIZE) {
                throw new IllegalArgumentException("The attachments of " + RpcContext.class.getSimpleName() + " is too big");
            }

            String attachments = key + "=" + value;
            requestTemplate.addHeader(RestConstant.DUBBO_ATTACHMENT_HEADER, attachments);
        }
    }

    private boolean illegalHttpHeaderKey(String key) {
        if (StringUtils.isNotEmpty(key)) {
            return key.contains(",") || key.contains("=");
        }
        return false;
    }

    private boolean illegalHttpHeaderValue(String value) {
        if (StringUtils.isNotEmpty(value)) {
            return value.contains(",");
        }
        return false;
    }
}
