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
import org.apache.dubbo.rpc.Invoker;

import java.util.List;
import java.util.stream.Collectors;

public class RouterSnapshotNode<T> {
    private final String name;
    private final int beforeSize;
    private int afterSize;
    private String routerMessage;
    private List<Invoker<T>> outputInvokers;
    private RouterSnapshotNode<T> nextNode;

    public RouterSnapshotNode(String name, int beforeSize) {
        this.name = name;
        this.beforeSize = beforeSize;
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

    public RouterSnapshotNode<T> getNextNode() {
        return nextNode;
    }

    public void appendNode(RouterSnapshotNode<T> nextNode) {
        if (this.nextNode != null) {
            RouterSnapshotNode<T> node = this.nextNode;
            while (node.nextNode != null) {
                node = node.nextNode;
            }
            node.nextNode = nextNode;
        } else {
            this.nextNode = nextNode;
        }
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
            .append("invokers: ")
            .append(beforeSize).append(" -> ").append(afterSize)
            .append(" ")
            .append(routerMessage == null ? "" : routerMessage)
            .append("] ")
            .append(outputInvokers == null ? "" :
                outputInvokers.subList(0, Math.min(5, outputInvokers.size()))
                    .stream()
                    .map(Invoker::getUrl)
                    .map(URL::getAddress)
                    .collect(Collectors.joining(",")));

        if (outputInvokers != null && outputInvokers.size() > 5) {
            stringBuilder.append("...");
        }
        if (nextNode != null) {
            stringBuilder.append("\n");
            for (int i = 0; i < level; i++) {
                stringBuilder.append("  ");
            }
            stringBuilder.append(nextNode.toString(level + 1));
        }
        return stringBuilder.toString();
    }
}
