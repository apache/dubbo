package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;
import org.apache.dubbo.rpc.model.ModuleModel;

@Activate(group = "consumer", order = Integer.MIN_VALUE + 9000)
public class AuthenticationConsumerFilter implements ClusterFilter {
    private volatile AuthorityIdentityFactory authorityIdentityFactory;

    private final ModuleModel moduleModel;

    public AuthenticationConsumerFilter(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        obtainAuthorityIdentityFactory();
        if (authorityIdentityFactory == null) {
            return invoker.invoke(invocation);
        }

        IdentityInfo identityInfo = authorityIdentityFactory.generateIdentity();

        invocation.setAttachment("authorization", identityInfo.getToken());

        return invoker.invoke(invocation);
    }

    private void obtainAuthorityIdentityFactory() {
        if (authorityIdentityFactory == null) {
            authorityIdentityFactory = moduleModel.getApplicationModel().getFrameworkModel()
                .getBeanFactory().getBean(AuthorityIdentityFactory.class);
        }
    }
}
