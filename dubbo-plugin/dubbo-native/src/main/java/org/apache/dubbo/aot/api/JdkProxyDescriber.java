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
package org.apache.dubbo.aot.api;

import java.util.List;
import java.util.Objects;

/**
 * A describer that describes the need for a JDK interface-based {@link java.lang.reflect.Proxy}.
 */
public class JdkProxyDescriber implements ConditionalDescriber {

    private final List<String> proxiedInterfaces;

    private final String reachableType;

    public JdkProxyDescriber(List<String> proxiedInterfaces, String reachableType) {
        this.proxiedInterfaces = proxiedInterfaces;
        this.reachableType = reachableType;
    }

    public List<String> getProxiedInterfaces() {
        return proxiedInterfaces;
    }

    @Override
    public String getReachableType() {
        return reachableType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JdkProxyDescriber that = (JdkProxyDescriber) o;
        return Objects.equals(proxiedInterfaces, that.proxiedInterfaces)
                && Objects.equals(reachableType, that.reachableType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proxiedInterfaces, reachableType);
    }
}
