/*
 * Copyright 1999-2101 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.common.util;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.common.domain.LoadBalance;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.domain.Weight;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * OverrideUtils.java
 *
 * @author tony.chenl
 */
public class OverrideUtils {
    public static List<Weight> overridesToWeights(List<Override> overrides) {
        List<Weight> weights = new ArrayList<Weight>();
        if (overrides == null) {
            return weights;
        }
        for (Override o : overrides) {
            if (StringUtils.isEmpty(o.getParams())) {
                continue;
            } else {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (entry.getKey().equals("weight")) {
                        Weight weight = new Weight();
                        weight.setAddress(o.getAddress());
                        weight.setId(o.getId());
                        weight.setService(o.getService());
                        weight.setWeight(Integer.valueOf(entry.getValue()));
                        weights.add(weight);
                    }
                }
            }
        }
        return weights;
    }

    public static Weight overrideToWeight(Override override) {
        return overridesToWeights(Arrays.asList(override)).get(0);
    }

    public static Override weightToOverride(Weight weight) {
        Override override = new Override();
        override.setId(weight.getId());
        override.setAddress(weight.getAddress());
        override.setEnabled(true);
        override.setParams("weight=" + weight.getWeight());
        override.setService(weight.getService());
        return override;
    }

    public static List<LoadBalance> overridesToLoadBalances(List<Override> overrides) {
        List<LoadBalance> loadBalances = new ArrayList<LoadBalance>();
        if (overrides == null) {
            return loadBalances;
        }
        for (Override o : overrides) {
            if (StringUtils.isEmpty(o.getParams())) {
                continue;
            } else {
                Map<String, String> params = StringUtils.parseQueryString(o.getParams());
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (entry.getKey().endsWith("loadbalance")) {
                        LoadBalance loadBalance = new LoadBalance();
                        String method = null;
                        if (entry.getKey().endsWith(".loadbalance")) {
                            method = entry.getKey().split(".loadbalance")[0];
                        } else {
                            method = "*";
                        }

                        loadBalance.setMethod(method);
                        loadBalance.setId(o.getId());
                        loadBalance.setService(o.getService());
                        loadBalance.setStrategy(entry.getValue());
                        loadBalances.add(loadBalance);

                    }
                }
            }
        }
        return loadBalances;
    }

    public static LoadBalance overrideToLoadBalance(Override override) {
        return OverrideUtils.overridesToLoadBalances(Arrays.asList(override)).get(0);
    }

    public static Override loadBalanceToOverride(LoadBalance loadBalance) {
        Override override = new Override();
        override.setId(loadBalance.getId());
        override.setService(loadBalance.getService());
        override.setEnabled(true);
        String method = loadBalance.getMethod();
        String strategy = loadBalance.getStrategy();
        if (StringUtils.isEmpty(method) || method.equals("*")) {
            override.setParams("loadbalance=" + strategy);
        } else {
            override.setParams(method + ".loadbalance=" + strategy);
        }
        return override;
    }

}
