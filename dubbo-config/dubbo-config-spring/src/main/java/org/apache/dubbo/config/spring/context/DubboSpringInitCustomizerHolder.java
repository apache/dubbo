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
package org.apache.dubbo.config.spring.context;

import java.util.HashSet;
import java.util.Set;

/**
 * Hold a set of DubboSpringInitCustomizer, for register customizers by programing.
 * <p>All customizers are store in thread local, and they will be clear after apply once.</p>
 *
 * <p>Usages:</p>
 *<pre>
 * DubboSpringInitCustomizerHolder.get().addCustomizer(context -> {...});
 * ClassPathXmlApplicationContext providerContext = new ClassPathXmlApplicationContext(..);
 * ...
 * DubboSpringInitCustomizerHolder.get().addCustomizer(context -> {...});
 * ClassPathXmlApplicationContext consumerContext = new ClassPathXmlApplicationContext(..);
 * </pre>
 */
public class DubboSpringInitCustomizerHolder {

    private static final ThreadLocal<DubboSpringInitCustomizerHolder> holders = ThreadLocal.withInitial(() ->
        new DubboSpringInitCustomizerHolder());

    public static DubboSpringInitCustomizerHolder get() {
        return holders.get();
    }

    private Set<DubboSpringInitCustomizer> customizers = new HashSet<>();

    public void addCustomizer(DubboSpringInitCustomizer customizer) {
        this.customizers.add(customizer);
    }

    public void clearCustomizers() {
        this.customizers = new HashSet<>();
    }

    public Set<DubboSpringInitCustomizer> getCustomizers() {
        return customizers;
    }

}
