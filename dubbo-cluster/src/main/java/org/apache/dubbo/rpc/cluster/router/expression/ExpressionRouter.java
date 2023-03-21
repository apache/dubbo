package org.apache.dubbo.rpc.cluster.router.expression;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.expression.context.ContextBuilder;
import org.apache.dubbo.rpc.cluster.router.expression.model.Rule;
import org.apache.dubbo.rpc.cluster.router.expression.model.RuleSet;
import org.apache.dubbo.rpc.cluster.router.expression.model.ExpressionRuleConstructor;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ExpressionRouter<T> extends ObserverRouter<T> {

    public static final String NAME = "expression";

    private static final Logger logger = LoggerFactory.getLogger(ExpressionRouter.class);

    /**
     * Store the mapping relations of provider/ruleSet.
     */
    private static final Map<String, RuleSet> ruleSets = new ConcurrentHashMap<>();

    private static final JexlEngine engine = new JexlBuilder().create();

    private ContextBuilder contextBuilder;

    public ExpressionRouter(URL url) {
        super(url, url.getParameter(CommonConstants.APPLICATION_KEY));
        contextBuilder = ExtensionLoader.getExtensionLoader(ContextBuilder.class)
            .getExtension(url.getParameter(Constants.CONTEXT_BUILDER_KEY, Constants.DEFAULT_CONTEXT_BUILDER));
    }

    @Override
    protected BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder, Holder<String> messageHolder) throws RpcException {
        String application = url.getParameter(CommonConstants.REMOTE_APPLICATION_KEY);
        if(application == null){
            return invokers;
        }
        RuleSet ruleSet = ruleSets.get(application);
        if (logger.isTraceEnabled()) {
            logger.trace(ruleSet.toString());
        }
        if (ruleSet != null && ruleSet.isEnabled()) {
            JexlContext clientContext = new MapContext();
            contextBuilder.buildClientContext(url, invocation).forEach(clientContext::set);
            for (Rule rule : ruleSet.getRules()) {
                Object clientQualified = engine.createExpression(rule.getClientCondition()).evaluate(clientContext);
                if (clientQualified instanceof Boolean && (Boolean) clientQualified) {
                    List<Invoker<T>> result = invokers
                        .stream()
                        .filter(invoker -> matches(contextBuilder.buildServerContext(invoker, url, invocation), rule.getServerQuery()))
                        .collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(result)) {
                        return result;
                    }
                }
            }
            if (ruleSet.isDefaultRuleEnabled()) {
                return invokers;
            } else {
                return new BitList<Invoker<T>>(new ArrayList<>());
            }
        }
        return invokers;
    }

    public boolean matches(Map<String, Object> objects, String expression) {
        JexlContext context = new MapContext();
        objects.forEach(context::set);
        Object qualified = engine.createExpression(expression).evaluate(context);
        return qualified instanceof Boolean && (Boolean) qualified;
    }

}
