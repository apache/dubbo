package com.alibaba.dubbo.rpc.cluster.router.unit;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.router.condition.ConditionRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * Support consumers to call the same unit of service priority
 *
 * @author yiji.github@hotmail.com
 */
public class UnitRouter extends ConditionRouter implements Router {

    private static final Logger logger = LoggerFactory.getLogger(UnitRouter.class);

    /**
     *  we expect consumer should invoker providers that has same value of `unit` property.
     */
    public static final URL ROUTER_URL =
            new URL("condition"
                    , Constants.ANYHOST_VALUE, 0
                    , Constants.ANY_VALUE )
                    . addParameters(
                         Constants.RULE_KEY, URL.encode("=> unit = $unit & methods = $methods")
                    );

    public UnitRouter() {
        this(ROUTER_URL);
    }

    private UnitRouter(URL url) {
        super(ROUTER_URL);
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation)
            throws RpcException {
        if (invokers == null || invokers.isEmpty()) {
            return invokers;
        }
        try {
            if (!matchWhen(url, invocation)) {
                return invokers;
            }
            List<Invoker<T>> result = new ArrayList<Invoker<T>>();

            for (Invoker<T> invoker : invokers) {
                if (matchThen(invoker.getUrl(), url, invocation)) {
                    result.add(invoker);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        } catch (Throwable t) {
            logger.error("Failed to execute unit router rule: " + getUrl()
                    + ", invokers: " + invokers
                    + ", consumer unit: " + url.getParameter(Constants.UNIT_KEY, Constants.DEFAULT_UNIT)
                    + ", cause: " + t.getMessage(), t);
        }
        return invokers;
    }

    public boolean matchThen(URL url, URL param, Invocation invocation) {
        return !(thenCondition == null || thenCondition.isEmpty()) && matchCondition(thenCondition, url, param, invocation);
    }

    @Override
    public boolean matchCondition(Map<String, ConditionRouter.MatchPair> condition, URL url, URL param, Invocation invocation) {

        Map<String, String> sample = url.toMap();
        boolean matched = false;
        for (Map.Entry<String, ConditionRouter.MatchPair> matchPair : condition.entrySet()) {
            String key = matchPair.getKey();
            String sampleValue ;

            URL consumerUrl = param == null ? url : param;
            // check if we are matching provider conditions
            boolean providerCondition = Constants.METHODS_KEY.equals(key) && consumerUrl != url;

            //get real invoked method name from invocation
            if (invocation != null && (Constants.METHOD_KEY.equals(key) || Constants.METHODS_KEY.equals(key))) {
                sampleValue = invocation.getMethodName();

                if(sampleValue == null) return false;

                if(providerCondition) {
                    String serviceMethod = sample.get(key);
                    matched = strictMatch(serviceMethod, sampleValue);

                    if(matched) continue;

                    return false;
                }
            }

            sampleValue = sample.get(key);
            if (sampleValue == null) {
                sampleValue = sample.get(Constants.DEFAULT_KEY_PREFIX + key);
            }

            if (!matchPair.getValue().isMatch(sampleValue, consumerUrl)) {
                return false;
            }
            matched = true;
        }
        return matched;
    }

    private boolean strictMatch(String serviceMethod, String invokedMethod){

        if(serviceMethod == null) return false;

        if(serviceMethod.indexOf(Constants.COMMA_SEPARATOR) >= 0) {
            String[] methods = serviceMethod.split(Constants.COMMA_SEPARATOR);
            for(String method : methods) {
                if(method.equals(invokedMethod)) return true;
            }
        }

        return serviceMethod.equals(invokedMethod);
    }
}
