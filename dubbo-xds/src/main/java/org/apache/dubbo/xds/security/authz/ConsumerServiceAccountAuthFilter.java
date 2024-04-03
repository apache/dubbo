package org.apache.dubbo.xds.security.authz;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.xds.security.api.ServiceAccountSource;

import java.util.Arrays;
import java.util.List;

@Activate(group = CommonConstants.CONSUMER,order = -10000)
public class ConsumerServiceAccountAuthFilter implements Filter {

    private final ServiceAccountSource accountJwtSource;

    public ConsumerServiceAccountAuthFilter(ApplicationModel applicationModel){
        this.accountJwtSource = applicationModel.getAdaptiveExtension(ServiceAccountSource.class);
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String security = invoker.getUrl().getParameter("security");
        if(StringUtils.isNotEmpty(security)){
            List<String> parts = Arrays.asList(security.split(","));
            boolean enable = parts.stream()
                    .anyMatch("sa_jwt"::equals);
            if(enable) {
                invocation.setObjectAttachment("authz",  accountJwtSource.getSaJwt(invoker.getUrl()));
            }
        }
        return invoker.invoke(invocation);
    }
}
