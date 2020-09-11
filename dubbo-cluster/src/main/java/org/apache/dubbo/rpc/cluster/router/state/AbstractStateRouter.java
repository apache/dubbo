package org.apache.dubbo.rpc.cluster.router.state;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;

public abstract class AbstractStateRouter implements StateRouter {
    protected int priority = DEFAULT_PRIORITY;
    protected boolean force = false;
    protected URL url;
    protected List<Invoker> invokers;

    protected AtomicReference<AddrCache> cache;

    protected GovernanceRuleRepository ruleRepository;


    public AbstractStateRouter(URL url) {
        this.ruleRepository = ExtensionLoader.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension();
        this.url = url;
    }

    public AbstractStateRouter() {

    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        this.invokers = (List) invokers;

    }

    @Override
    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

}
