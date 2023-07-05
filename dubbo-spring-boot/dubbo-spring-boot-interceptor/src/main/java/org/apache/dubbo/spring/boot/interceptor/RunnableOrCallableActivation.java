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
package org.apache.dubbo.spring.boot.interceptor;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import org.apache.dubbo.spring.boot.toolkit.DubboCrossThread;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class RunnableOrCallableActivation {
    // add '_' before dubboTag to avoid conflict field name
    public static final String FIELD_NAME_DUBBO_TAG = "_dubboTag";
    private static final String CALL_METHOD_NAME = "call";
    private static final String RUN_METHOD_NAME = "run";
    private static final String APPLY_METHOD_NAME = "apply";
    private static final String ACCEPT_METHOD_NAME = "accept";

    public static void install(Instrumentation instrumentation) {
        new AgentBuilder.Default()
            .type(isAnnotatedWith(DubboCrossThread.class))
            .transform(new AgentBuilder.Transformer() {
                @Override
                public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                                                        ClassLoader classLoader, JavaModule module,
                                                        ProtectionDomain protectionDomain) {
                    return builder
                        .defineField(FIELD_NAME_DUBBO_TAG, String.class, Visibility.PUBLIC)
                        .method(
                            ElementMatchers.named(RUN_METHOD_NAME).and(takesArguments(0))
                                .or(ElementMatchers.named(CALL_METHOD_NAME).and(takesArguments(0)))
                                .or(ElementMatchers.named(APPLY_METHOD_NAME).and(takesArguments(0)))
                                .or(ElementMatchers.named(ACCEPT_METHOD_NAME).and(takesArguments(0)))
                        )
                        .intercept(Advice.to(RunnableOrCallableMethodInterceptor.class))
                        .constructor(ElementMatchers.any())
                        .intercept(Advice.to(RunnableOrCallableConstructInterceptor.class));
                }
            })
            .installOn(instrumentation);
    }
}
