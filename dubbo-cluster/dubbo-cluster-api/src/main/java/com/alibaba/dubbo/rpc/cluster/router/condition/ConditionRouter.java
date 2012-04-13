/*
 * Copyright 1999-2012 Alibaba Group.
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
package com.alibaba.dubbo.rpc.cluster.router.condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;

/**
 * ConditionRouter
 * 
 * @author william.liangf
 */
public class ConditionRouter implements Router, Comparable<Router> {

    private final int priority;

    private final String rule;
    
    private final URL url;

    public URL getUrl() {
        return url;
    }

    public ConditionRouter(URL url) {
        this.url = url;
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
        String r = url.getParameterAndDecoded(Constants.RULE_KEY, "");
        this.rule = "service=" + url.getPath() + (r.trim().startsWith("=>") ? "" : "&") + r;
    }

    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation)
            throws RpcException {
        if (invokers == null || invokers.size() == 0) {
            return invokers;
        }
        Map<String, String> urls = new HashMap<String, String>();
        Map<URL, Invoker<T>> urlInvokers = new HashMap<URL, Invoker<T>>();
        for (Invoker<T> invoker : invokers) {
            urls.put(invoker.getUrl().toIdentityString(), invoker.getUrl().toParameterString());
            urlInvokers.put(invoker.getUrl(), invoker);
        }
        Map<String, String> routedUrls = RouteUtils.route(null, url.getServiceKey(), url.getAddress(), url.toParameterString(), urls, Arrays.asList(rule), null);
        if (routedUrls != null) {
            List<Invoker<T>> result = new ArrayList<Invoker<T>>();
            for (Map.Entry<String, String> entry : routedUrls.entrySet()) {
                result.add(urlInvokers.get(URL.valueOf(entry.getKey() + "?" + entry.getValue())));
            }
            return result;
        }
        return invokers;
    }

    public int compareTo(Router o) {
        if (o == null || o.getClass() != ConditionRouter.class) {
            return 1;
        }
        ConditionRouter c = (ConditionRouter) o;
        return this.priority == c.priority ? rule.compareTo(c.rule) : (this.priority > c.priority ? 1 : -1);
    }

}
