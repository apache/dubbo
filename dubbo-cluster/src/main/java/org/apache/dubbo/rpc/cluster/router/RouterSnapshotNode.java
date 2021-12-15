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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class RouterSnapshotNode<T> {
    private final String name;
    private final int beforeSize;
    private int afterSize;
    private String routerMessage;
    private final List<Invoker<T>> inputInvokers;
    private List<Invoker<T>> outputInvokers;
    private final List<RouterSnapshotNode<T>> nextNode = new LinkedList<>();

    public RouterSnapshotNode(String name, List<Invoker<T>> inputInvokers) {
        this.name = name;
        this.beforeSize = inputInvokers.size();
        if (inputInvokers instanceof BitList) {
            this.inputInvokers = inputInvokers;
        } else {
            this.inputInvokers = new ArrayList<>(5);
            for (int i = 0; i < Math.min(5, beforeSize); i++) {
                this.inputInvokers.add(inputInvokers.get(i));
            }
        }
    }

    public String getName() {
        return name;
    }

    public int getBeforeSize() {
        return beforeSize;
    }

    public int getAfterSize() {
        return afterSize;
    }

    public String getRouterMessage() {
        return routerMessage;
    }

    public void setRouterMessage(String routerMessage) {
        this.routerMessage = routerMessage;
    }

    public List<Invoker<T>> getOutputInvokers() {
        return outputInvokers;
    }

    public void setOutputInvokers(List<Invoker<T>> outputInvokers) {
        this.outputInvokers = outputInvokers;
        this.afterSize = outputInvokers == null ? 0 : outputInvokers.size();
    }

    public List<RouterSnapshotNode<T>> getNextNode() {
        return nextNode;
    }

    public void appendNode(RouterSnapshotNode<T> nextNode) {
        this.nextNode.add(nextNode);
    }

    @Override
    public String toString() {
        return toString(1);
    }

    public String toString(int level) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ ")
            .append(name)
            .append(" ")
            .append("Invokers: ")
            .append(beforeSize).append(" -> ").append(afterSize)
            .append(routerMessage == null ? "" : " Router message: ")
            .append(routerMessage == null ? "" : routerMessage)
            .append(" ] ")
            .append(CollectionUtils.isEmpty(inputInvokers) ? "Empty" :
                inputInvokers.subList(0, Math.min(5, inputInvokers.size()))
                    .stream()
                    .map(Invoker::getUrl)
                    .map(URL::getAddress)
                    .collect(Collectors.joining(",")))
            .append(" -> ")
            .append(CollectionUtils.isEmpty(outputInvokers) ? "Empty" :
                outputInvokers.subList(0, Math.min(5, outputInvokers.size()))
                    .stream()
                    .map(Invoker::getUrl)
                    .map(URL::getAddress)
                    .collect(Collectors.joining(",")));

        if (outputInvokers != null && outputInvokers.size() > 5) {
            stringBuilder.append("...");
        }
        for (RouterSnapshotNode<T> node : nextNode) {
            stringBuilder.append("\n");
            for (int i = 0; i < level; i++) {
                stringBuilder.append("  ");
            }
            stringBuilder.append(node.toString(level + 1));
        }
        return stringBuilder.toString();
    }
}
