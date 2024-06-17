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
package org.apache.dubbo.rpc.protocol.tri.rest.support.basic;

import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationEnum;

import java.lang.annotation.Annotation;

public enum Annotations implements AnnotationEnum {
    Mapping(org.apache.dubbo.remoting.http12.rest.Mapping.class),
    Param(org.apache.dubbo.remoting.http12.rest.Param.class),
    Nonnull(javax.annotation.Nonnull.class);

    private final Class<Annotation> type;

    @SuppressWarnings("unchecked")
    Annotations(Class<? extends Annotation> type) {
        this.type = (Class<Annotation>) type;
    }

    public String className() {
        return type.getName();
    }

    @Override
    public Class<Annotation> type() {
        return type;
    }
}
