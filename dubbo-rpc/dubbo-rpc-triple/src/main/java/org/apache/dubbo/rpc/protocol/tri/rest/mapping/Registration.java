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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping;

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.HandlerMeta;

public final class Registration {

    private final RequestMapping mapping;
    private final HandlerMeta meta;

    public Registration(RequestMapping mapping, HandlerMeta meta) {
        this.mapping = mapping;
        this.meta = meta;
    }

    public RequestMapping getMapping() {
        return mapping;
    }

    public HandlerMeta getMeta() {
        return meta;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != Registration.class) {
            return false;
        }
        return mapping.equals(((Registration) obj).mapping);
    }

    @Override
    public int hashCode() {
        return mapping.hashCode();
    }

    @Override
    public String toString() {
        return "Registration{mapping=" + mapping + ", method=" + meta.getMethod() + '}';
    }
}
