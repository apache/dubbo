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
import org.apache.dubbo.rpc.Invoker;

import java.util.Map;
import java.util.stream.Collectors;

public class RouterGroupingState<T> {
    private final String routerName;
    private final int total;
    private final Map<String, BitList<Invoker<T>>> grouping;

    public RouterGroupingState(String routerName, int total, Map<String, BitList<Invoker<T>>> grouping) {
        this.routerName = routerName;
        this.total = total;
        this.grouping = grouping;
    }

    public String getRouterName() {
        return routerName;
    }

    public int getTotal() {
        return total;
    }

    public Map<String, BitList<Invoker<T>>> getGrouping() {
        return grouping;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(routerName)
            .append(' ')
            .append(" Total: ")
            .append(total)
            .append("\n");

        for (Map.Entry<String, BitList<Invoker<T>>> entry : grouping.entrySet()) {
            BitList<Invoker<T>> invokers = entry.getValue();
            stringBuilder.append("[ ")
                .append(entry.getKey())
                .append(" -> ")
                .append(invokers.isEmpty() ?
                    "Empty" :
                    invokers.stream()
                        .limit(5)
                        .map(Invoker::getUrl)
                        .map(URL::getAddress)
                        .collect(Collectors.joining(",")))
                .append(invokers.size() > 5 ? "..." : "")
                .append(" (Total: ")
                .append(invokers.size())
                .append(") ]")
                .append("\n");
        }
        return stringBuilder.toString();
    }
}
