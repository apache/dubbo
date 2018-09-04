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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class InvokerTreeCache<T> {

    TreeNode<T> tree;

    public TreeNode buildTree() {
        tree = new TreeNode<>();
        return tree;
    }

    public List<Invoker<T>> getInvokers(TreeNode<T> node, URL url, Invocation invocation) {
        if (node.getChildren() == null || node.getChildren().size() == 0) {
            return node.getInvokers();
        }

        if (node.getChildren().size() == 1) {
            return getInvokers(node.getChildren().get(0), url, invocation);
        }

        TreeNode<T> failoverNode = null;
        for (TreeNode<T> n : node.getChildren()) {
            String key = n.getConditionKey();
            if (TreeNode.FAILOVER_KEY.equals(key)) {
                failoverNode = n;
                continue;
            }

            //TODO key=method, but it will appear neither in url nor in attachments.
            String value = invocation.getAttachment(key, url.getParameter(key));
            if (key.equals("method")) {
                value = invocation.getMethodName();
            }

            if (n.getConditionValue().equals(value)) {
                if (n.getInvokers() != null || (n.getInvokers() == null && n.isForce()))
                    return getInvokers(n, url, invocation);
            }
        }

        if (failoverNode != null) {
            return getInvokers(failoverNode, url, invocation);
        }
        return Collections.emptyList();
    }


    public TreeNode getTree() {
        return tree;
    }

}
