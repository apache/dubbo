package org.apache.dubbo.rpc.cluster.router.expression;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;

public abstract class ObserverRouter<T> extends AbstractStateRouter<T> implements ConfigurationListener {
    public static final String NAME = "OBSERVER_ROUTER";
    private static final String RULE_SUFFIX = ".observer-router";

    public ObserverRouter(URL url, String ruleKey) {
        super(url);
        this.init(ruleKey);
    }

    private synchronized void init(String ruleKey) {
        if (StringUtils.isNotEmpty(ruleKey)) {
            String routerKey = ruleKey + RULE_SUFFIX;
            ruleRepository.addListener(routerKey, this);
            String rule = ruleRepository.getRule(routerKey, DynamicConfiguration.DEFAULT_GROUP);
            if (StringUtils.isNotEmpty(rule)) {
                this.process(new ConfigChangedEvent(routerKey, DynamicConfiguration.DEFAULT_GROUP, rule));
            }
        }
    }
}
