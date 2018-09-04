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

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TreeNode<T> {
    public static final String FAILOVER_KEY = "failover";

    private String routerName;
    private String conditionKey;
    private String conditionValue;
    private boolean force;
    private List<Invoker<T>> invokers;
    private List<TreeNode<T>> children;

    public TreeNode() {
        this.children = new ArrayList<>();
    }

    public TreeNode(String routerName, String conditionKey, String conditionValue, List<Invoker<T>> invokers, boolean force) {
        this.routerName = routerName;
        this.conditionKey = conditionKey;
        this.conditionValue = conditionValue;
        this.invokers = invokers;
        this.force = force;
        this.children = new ArrayList<>();
    }

    public void traverse() {

    }

    public void addChild(TreeNode child) {
        children.add(child);
    }

    public boolean isLeaf() {
        if (CollectionUtils.isEmpty(children)) {
            return true;
        }
        return false;
    }

    public String getRouterName() {
        return routerName;
    }

    public void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    public String getConditionKey() {
        return conditionKey;
    }

    public void setConditionKey(String conditionKey) {
        this.conditionKey = conditionKey;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public List<Invoker<T>> getInvokers() {
        return invokers;
    }

    public void setInvokers(List<Invoker<T>> invokers) {
        this.invokers = invokers;
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode<T>> children) {
        this.children = children;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }
}
