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
package com.alibaba.dubbo.rpc.cluster.router.script;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.Router;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScriptRouter
 *
 * 基于脚本的 Router 实现类
 */
public class ScriptRouter implements Router {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRouter.class);

    /**
     * 脚本类型 与 ScriptEngine 的映射缓存
     */
    private static final Map<String, ScriptEngine> engines = new ConcurrentHashMap<String, ScriptEngine>();

    /**
     * 路由规则 URL
     */
    private final ScriptEngine engine;
    /**
     * 路由规则的优先级，用于排序，优先级越大越靠前执行，可不填，缺省为 0 。
     */
    private final int priority;
    /**
     * 路由规则内容
     */
    private final String rule;
    /**
     * 路由规则 URL
     */
    private final URL url;

    public ScriptRouter(URL url) {
        this.url = url;
        String type = url.getParameter(Constants.TYPE_KEY);
        this.priority = url.getParameter(Constants.PRIORITY_KEY, 0);
        String rule = url.getParameterAndDecoded(Constants.RULE_KEY);
        // 初始化 `engine`
        if (type == null || type.length() == 0) {
            type = Constants.DEFAULT_SCRIPT_TYPE_KEY;
        }
        if (rule == null || rule.length() == 0) {
            throw new IllegalStateException(new IllegalStateException("route rule can not be empty. rule:" + rule));
        }
        ScriptEngine engine = engines.get(type);
        if (engine == null) { // 在缓存中不存在，则进行创建 ScriptEngine 对象
            engine = new ScriptEngineManager().getEngineByName(type);
            if (engine == null) {
                throw new IllegalStateException(new IllegalStateException("Unsupported route rule type: " + type + ", rule: " + rule));
            }
            engines.put(type, engine);
        }
        this.engine = engine;
        this.rule = rule;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        try {
            // 执行脚本
            List<Invoker<T>> invokersCopy = new ArrayList<Invoker<T>>(invokers);
            Compilable compilable = (Compilable) engine;
            Bindings bindings = engine.createBindings();
            bindings.put("invokers", invokersCopy);
            bindings.put("invocation", invocation);
            bindings.put("context", RpcContext.getContext());
            CompiledScript function = compilable.compile(rule); // 编译
            Object obj = function.eval(bindings); // 执行
            // 根据结果类型，转换成 (List<Invoker<T>> 类型返回
            if (obj instanceof Invoker[]) {
                invokersCopy = Arrays.asList((Invoker<T>[]) obj);
            } else if (obj instanceof Object[]) {
                invokersCopy = new ArrayList<Invoker<T>>();
                for (Object inv : (Object[]) obj) {
                    invokersCopy.add((Invoker<T>) inv);
                }
            } else {
                invokersCopy = (List<Invoker<T>>) obj;
            }
            return invokersCopy;
        } catch (ScriptException e) {
            // 发生异常，忽略路由规则，返回全 `invokers` 集合
            // fail then ignore rule .invokers.
            logger.error("route error , rule has been ignored. rule: " + rule + ", method:" + invocation.getMethodName() + ", url: " + RpcContext.getContext().getUrl(), e);
            return invokers;
        }
    }

    @Override
    public int compareTo(Router o) {
        if (o == null || o.getClass() != ScriptRouter.class) {
            return 1;
        }
        ScriptRouter c = (ScriptRouter) o;
        return this.priority == c.priority ? rule.compareTo(c.rule) : (this.priority > c.priority ? 1 : -1);
    }

}