package org.apache.dubbo.config.security;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.rpc.model.ScopeModelUtil;
import org.apache.dubbo.rpc.model.ServiceModel;

import java.util.List;
import java.util.Optional;

@Activate(order = 200)
public class SecurityProtocolWrapper implements Protocol, ScopeModelAware {
    private final Protocol protocol;

    public SecurityProtocolWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    @Override
    public void setFrameworkModel(FrameworkModel frameworkModel) {
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        ServiceModel serviceModel = invoker.getUrl().getServiceModel();
        ScopeModel scopeModel = invoker.getUrl().getScopeModel();
        Optional.ofNullable(serviceModel).map(ServiceModel::getServiceInterfaceClass)
            .ifPresent((interfaceClass) -> {
                SecurityManager securityManager = ScopeModelUtil.getFrameworkModel(scopeModel)
                    .getBeanFactory().getBean(SecurityManager.class);
                securityManager.registerInterface(interfaceClass);
            });
        return protocol.export(invoker);
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        ServiceModel serviceModel = url.getServiceModel();
        ScopeModel scopeModel = url.getScopeModel();
        SecurityManager securityManager = ScopeModelUtil.getFrameworkModel(scopeModel)
            .getBeanFactory().getBean(SecurityManager.class);

        Optional.ofNullable(serviceModel).map(ServiceModel::getServiceInterfaceClass)
            .ifPresent(securityManager::registerInterface);
        securityManager.registerInterface(type);

        return protocol.refer(type, url);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }

    @Override
    public List<ProtocolServer> getServers() {
        return protocol.getServers();
    }
}
