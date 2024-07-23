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
package org.apache.dubbo.rpc.cluster.router.condition.config.model;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

public class Destination<T> {
    private int weight;
    private BitList<Invoker<T>> invokers;

    Destination(int weight, BitList<Invoker<T>> invokers) {
        this.weight = weight;
        this.invokers = invokers;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public BitList<Invoker<T>> getInvokers() {
        return invokers;
    }

    public void setInvokers(BitList<Invoker<T>> invokers) {
        this.invokers = invokers;
    }
}
