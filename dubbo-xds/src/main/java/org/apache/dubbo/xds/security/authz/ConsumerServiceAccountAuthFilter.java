package org.apache.dubbo.xds.security.authz;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.kubernetes.KubeEnv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Activate(group = CommonConstants.CONSUMER,order = 100000)
public class ConsumerServiceAccountAuthFilter implements Filter {

    private final KubeEnv kubeEnv;

    public ConsumerServiceAccountAuthFilter(ApplicationModel applicationModel){
        this.kubeEnv = applicationModel.getBeanFactory()
                .getBean(KubeEnv.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            String security =invoker.getUrl().getParameter("security");
            if(StringUtils.isNotEmpty(security)){
                List<String> parts = Arrays.asList(security.split(","));
                boolean enable = parts.stream()
                        .anyMatch("sa_jwt"::equals);
                if(enable) {
                    RpcContext.getClientAttachment()
                            .setAttachment("Authorization", new String(kubeEnv.getServiceAccountToken(), StandardCharsets.UTF_8));
                }
            }
        }catch (IOException e){
            throw new RpcException("Failed to read SA JWT for current service.",e);
        }
        return invoker.invoke(invocation);
    }
}
