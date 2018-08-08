package org.apache.dubbo.rpc.cluster.router.tag;


import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kirito.moe@foxmail.com
 * Date 2018-08-03
 */
public class TagRouter implements Router, Comparable<Router> {

    private static final Logger logger = LoggerFactory.getLogger(TagRouter.class);

    private final int priority;
    private final URL url;

    public static final URL ROUTER_URL =
            new URL("tag"
                    , Constants.ANYHOST_VALUE, 0
                    , Constants.ANY_VALUE)
                    .addParameters(
                            Constants.RUNTIME_KEY, "true"
                    );

    public TagRouter(URL url) {
        this.url = url;
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
    }

    public TagRouter() {
        this.url = ROUTER_URL;
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        // filter
        List<Invoker<T>> result = new ArrayList<>();
        try {
            // static config
            String tag = url.getParameter(Constants.REQUEST_TAG_KEY);
            // dynamic param
            if (!StringUtils.isEmpty(RpcContext.getContext().getAttachment(Constants.REQUEST_TAG_KEY))) {
                tag = RpcContext.getContext().getAttachment(Constants.REQUEST_TAG_KEY);
            }
            // Tag request
            if (!StringUtils.isEmpty(tag)) {
                // Select tag invokers first
                for (Invoker<T> invoker : invokers) {
                    if (tag.equals(invoker.getUrl().getParameter(Constants.TAG_KEY))) {
                        result.add(invoker);
                    }
                }
                // If no invoker be selected, downgrade to normal invokers
                if (result.isEmpty()) {
                    for (Invoker<T> invoker : invokers) {
                        if (StringUtils.isEmpty(invoker.getUrl().getParameter(Constants.TAG_KEY))) {
                            result.add(invoker);
                        }
                    }
                }
                // Normal request
            } else {
                for (Invoker<T> invoker : invokers) {
                    if (StringUtils.isEmpty(invoker.getUrl().getParameter(Constants.TAG_KEY))) {
                        result.add(invoker);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("Route by tag error,return all invokers.", e);
        }
        // Downgrade to all invokers
        return invokers;
    }

    @Override
    public int compareTo(Router o) {
        if (o == null || o.getClass() != TagRouter.class) {
            return 1;
        }
        TagRouter c = (TagRouter) o;
        return this.priority == c.priority ? url.toFullString().compareTo(c.url.toFullString()) : (this.priority > c.priority ? 1 : -1);
    }
}
