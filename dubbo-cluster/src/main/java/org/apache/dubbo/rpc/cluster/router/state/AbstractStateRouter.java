/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.cluster.router.state;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Constants;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.model.ModuleModel;

/***
 * The abstract class of StateRoute.
 * @since 3.0
 */
public abstract class AbstractStateRouter<T> implements StateRouter<T> {
    private volatile boolean force = false;
    private volatile URL url;
    private volatile StateRouter<T> nextRouter = null;

    private final GovernanceRuleRepository ruleRepository;

    /**
     * Should continue route if current router's result is empty
     */
    private final boolean shouldFailFast;

    protected ModuleModel moduleModel;

    public AbstractStateRouter(URL url) {
        moduleModel = url.getOrDefaultModuleModel();
        this.ruleRepository = moduleModel.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension();
        this.url = url;
        this.shouldFailFast = Boolean.parseBoolean(ConfigurationUtils.getProperty(moduleModel, Constants.SHOULD_FAIL_FAST_KEY, "true"));
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

    public GovernanceRuleRepository getRuleRepository() {
        return this.ruleRepository;
    }

    public StateRouter<T> getNextRouter() {
        return nextRouter;
    }

    @Override
    public void notify(BitList<Invoker<T>> invokers) {
        // default empty implement
    }

    @Override
    public final BitList<Invoker<T>> route(BitList<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder) throws RpcException {
        if (needToPrintMessage && (nodeHolder == null || nodeHolder.get() == null)) {
            needToPrintMessage = false;
        }

        RouterSnapshotNode<T> currentNode = null;
        RouterSnapshotNode<T> parentNode = null;
        Holder<String> messageHolder = null;

        // pre-build current node
        if (needToPrintMessage) {
            parentNode = nodeHolder.get();
            currentNode = new RouterSnapshotNode<>(this.getClass().getSimpleName(), invokers.clone());
            parentNode.appendNode(currentNode);

            // set parent node's output size in the first child invoke
            // initial node output size is zero, first child will override it
            if (parentNode.getNodeOutputSize() < invokers.size()) {
                parentNode.setNodeOutputInvokers(invokers.clone());
            }

            messageHolder = new Holder<>();
            nodeHolder.set(currentNode);
        }
        BitList<Invoker<T>> routeResult;

        routeResult = doRoute(invokers, url, invocation, needToPrintMessage, nodeHolder, messageHolder);
        if (routeResult != invokers) {
            routeResult = invokers.and(routeResult);
        }
        // check if router support call continue route by itself
        if (!supportContinueRoute()) {
            // use current node's result as next node's parameter
            if (!shouldFailFast || !routeResult.isEmpty()) {
                routeResult = continueRoute(routeResult, url, invocation, needToPrintMessage, nodeHolder);
            }
        }

        // post-build current node
        if (needToPrintMessage) {
            currentNode.setRouterMessage(messageHolder.get());
            if (currentNode.getNodeOutputSize() == 0) {
                // no child call
                currentNode.setNodeOutputInvokers(routeResult.clone());
            }
            currentNode.setChainOutputInvokers(routeResult.clone());
            nodeHolder.set(parentNode);
        }
        return routeResult;
    }

    /**
     * Filter invokers with current routing rule and only return the invokers that comply with the rule.
     *
     * @param invokers all invokers to be routed
     * @param url consumerUrl
     * @param invocation invocation
     * @param needToPrintMessage should current router print message
     * @param nodeHolder RouterSnapshotNode In general, router itself no need to care this param, just pass to continueRoute
     * @param messageHolder message holder when router should current router print message
     * @return routed result
     */
    protected abstract BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation,
                                                boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder,
                                                Holder<String> messageHolder) throws RpcException;

    /**
     * Call next router to get result
     *
     * @param invokers current router filtered invokers
     */
    protected final BitList<Invoker<T>> continueRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation,
                                                      boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder) {
        if (nextRouter != null) {
            return nextRouter.route(invokers, url, invocation, needToPrintMessage, nodeHolder);
        } else {
            return invokers;
        }
    }

    /**
     * Whether current router's implementation support call
     * {@link AbstractStateRouter#continueRoute(BitList, URL, Invocation, boolean, Holder)}
     * by router itself.
     *
     * @return support or not
     */
    protected boolean supportContinueRoute() {
        return false;
    }

    /**
     * Next Router node state is maintained by AbstractStateRouter and this method is not allow to override.
     * If a specified router wants to control the behaviour of continue route or not,
     * please override {@link AbstractStateRouter#supportContinueRoute()}
     */
    @Override
    public final void setNextRouter(StateRouter<T> nextRouter) {
        this.nextRouter = nextRouter;
    }

    @Override
    public final String buildSnapshot() {
        return doBuildSnapshot() +
            "            v \n" +
            nextRouter.buildSnapshot();
    }

    protected String doBuildSnapshot() {
        return this.getClass().getSimpleName() + " not support\n";
    }
}
