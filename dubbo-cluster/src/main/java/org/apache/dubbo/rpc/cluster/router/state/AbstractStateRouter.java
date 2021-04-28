package org.apache.dubbo.rpc.cluster.router.state;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;

public abstract class AbstractStateRouter implements StateRouter {
    protected int priority = DEFAULT_PRIORITY;
    protected boolean force = false;
    protected URL url;
    protected List<Invoker> invokers;
    protected AtomicReference<AddrCache> cache;
    protected GovernanceRuleRepository ruleRepository;
    final protected RouterChain chain;

    public AbstractStateRouter(URL url, RouterChain chain) {
        this.ruleRepository = ExtensionLoader.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension();
        this.chain = chain;
        this.url = url;
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

    @Override
    public <T> BitList<Invoker<T>> route(BitList<Invoker<T>> invokers, RouterCache cache, URL url,
        Invocation invocation) throws RpcException {

        List<String> tags = getTags(url, invocation);

        if (tags == null) {
            return invokers;
        }
        for (String tag : tags) {
            BitList tagInvokers = cache.getAddrPool().get(tag);
            if (tagMatchFail(invokers)) {
                continue;
            }
            return tagInvokers.intersect(invokers, (List)invokers.getUnmodifiableList());
        }


        return invokers;
    }

    public List<String> getTags(URL url, Invocation invocation) {
        return new ArrayList<String>();
    }

    public <T> Boolean tagMatchFail(BitList<Invoker<T>> invokers) {
        return invokers.size() <= 0;
    }

    @Override
    public void pool() {
        chain.loop(false);
    }
}
