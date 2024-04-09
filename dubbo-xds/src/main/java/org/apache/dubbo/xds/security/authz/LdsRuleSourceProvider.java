package org.apache.dubbo.xds.security.authz;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.xds.listener.LdsListener;

import java.util.List;

import io.envoyproxy.envoy.config.listener.v3.Listener;

@Activate
public class LdsRuleSourceProvider implements RuleSourceProvider, LdsListener {

    @Override
    public List<RuleSource> getSource(URL url, Invocation invocation) {
        return null;
    }

    @Override
    public void onResourceUpdate(List<Listener> resource) {

    }
}
