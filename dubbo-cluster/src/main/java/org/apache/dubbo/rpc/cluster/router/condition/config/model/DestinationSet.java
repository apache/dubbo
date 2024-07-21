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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DestinationSet<T> {
    private final List<Destination<T>> destinations;
    private long weightSum;
    private final ThreadLocalRandom random;

    public DestinationSet() {
        this.destinations = new ArrayList<>();
        this.weightSum = 0;
        this.random = ThreadLocalRandom.current();
    }

    public void addDestination(int weight, BitList<Invoker<T>> invokers) {
        destinations.add(new Destination(weight, invokers));
        weightSum += weight;
    }

    public BitList<Invoker<T>> randDestination() {
        if (destinations.size() == 1) {
            return destinations.get(0).getInvokers();
        }

        long sum = random.nextLong(weightSum);
        for (Destination destination : destinations) {
            sum -= destination.getWeight();
            if (sum <= 0) {
                return destination.getInvokers();
            }
        }
        return BitList.emptyList();
    }

    public List<Destination<T>> getDestinations() {
        return destinations;
    }

    public long getWeightSum() {
        return weightSum;
    }

    public void setWeightSum(long weightSum) {
        this.weightSum = weightSum;
    }

    public Random getRandom() {
        return random;
    }
}
