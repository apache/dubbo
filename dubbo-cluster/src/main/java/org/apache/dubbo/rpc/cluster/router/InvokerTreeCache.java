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
package org.apache.dubbo.rpc.cluster.router;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class InvokerTreeCache<T> {

    private TreeNode<T> tree;

    public TreeNode buildTree() {
        tree = new TreeNode<>();
        tree.setRouterName("ROOT_ROUTER");
        tree.setConditionValue("root");
        tree.setConditionKey("root");
        return tree;
    }

    public List<Invoker<T>> getInvokers(TreeNode<T> node, URL url, Invocation invocation) {
        // We have reached the leaf node.
        if (node.isLeaf()) {
            return node.getInvokers();
        }

        //
       /* if (node.getChildren().size() == 1) {
            return getInvokers(node.getChildren().get(0), url, invocation);
        }*/

        TreeNode<T> failoverNode = null;
        for (TreeNode<T> n : node.getChildren()) {
            String key = n.getConditionKey();
            // if the key is FAILOVER, it indicates we have only one child in this level, just proceed on.
            if (TreeNode.FAILOVER_KEY.equals(key)) {
                return getInvokers(n, url, invocation);
            }
            if (TreeNode.FAILOVER_KEY.equals(n.getConditionValue())) {
                failoverNode = n;
                continue;
            }

            //TODO key=method, but it will appear neither in url nor in attachments.
            String value = invocation.getAttachment(key, url.getParameter(key));
            if (Constants.METHOD_KEY.equals(key)) {
                value = invocation.getMethodName();
            }

            // If we don't have a match condition in the request, then our goal would be find the failoverNode in this loop and continue match with failoverNode's children.
            if (StringUtils.isEmpty(value)) {
                if (failoverNode == null) {
                    for (TreeNode<T> innerLoopNode : node.getChildren()) {
                        if (innerLoopNode.getConditionValue().equals(TreeNode.FAILOVER_KEY)) {
                            failoverNode = innerLoopNode;
                        }
                    }
                }
                // Router will guarantee that there's always a FAILOVER node.
                // To make it more robust, we may need to add null check for failoverNode here.
                return getInvokers(failoverNode, url, invocation);
            }

            // If the request condition matches with the node branch, go ahead.
            if (n.getConditionValue().equals(value)) {
                // If the invoker list in this node is empty, we need to check force to decide to return empty list or to seek for FAILOVER.
                if (CollectionUtils.isNotEmpty(n.getInvokers()) || (CollectionUtils.isEmpty(n.getInvokers()) && n.isForce()))
                    return getInvokers(n, url, invocation);
            }
        }

        // If we get here,
        // 1. we must have several brothers in current node level.
        // 2. there is a router match condition in the request.
        // 3. the request value failed to match any of the values specified by the router rule.
        if (failoverNode != null) {
            // What if force parameter comes from runtime? Use a convention format of 'force.xxx', for example, for TagRouter it would be 'force.tag'.
            // FIXME check force logic here
            String forceKey = "force." + failoverNode.getConditionKey();
            if (Boolean.valueOf(invocation.getAttachment(forceKey, url.getParameter(forceKey, "false")))) {
                /**
                 * This may mistakenly return empty list for runtime routers
                 * see {@link org.apache.dubbo.rpc.cluster.router.tag.TagRouter.getKey()} for the workaround.
                 */
                return Collections.emptyList();
            }
            return getInvokers(failoverNode, url, invocation);
        }
        return Collections.emptyList();
    }


    public TreeNode getTree() {
        return tree;
    }

}
