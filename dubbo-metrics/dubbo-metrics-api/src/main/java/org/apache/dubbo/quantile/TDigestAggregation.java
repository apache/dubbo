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
package org.apache.dubbo.quantile;

import com.tdunning.math.stats.TDigest;
import org.apache.dubbo.metrices.Metrics;

import java.util.LinkedList;

public class TDigestAggregation implements Aggregation {
    @Override
    public Quantile getQuantile(LinkedList<Metrics> store) {
        Quantile rt = new Quantile();
        rt.setLast(getLast(store));
        TDigest tDigest = TDigest.createAvlTreeDigest(store.size() + 1);
        double sum = 0;
        double count = 0;
        for (Metrics metrics : store) {
            if (metrics.getStatus() == 2) {
                tDigest.add(metrics.getELSeconds());
                sum += metrics.getELSeconds();
                count++;
            }
        }
        rt.setAvg(sum / count);
        rt.setP99(tDigest.quantile(0.99));
        rt.setP95(tDigest.quantile(0.95));
        rt.setMax(tDigest.getMax());
        rt.setMin(tDigest.getMin());
        return rt;
    }

    private double getLast(LinkedList<Metrics> store) {
        int size = store.size();
        Metrics metrics = store.get(size - 1);
        while (metrics.getStatus() != 2) {
            metrics = store.get(--size);
        }
        return metrics.getELSeconds();
    }
}
