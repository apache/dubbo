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

package com.alibaba.com.caucho.hessian.io;

import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;

class EnumSetHandler implements Serializable, HessianHandle {
    private Class type;
    private Object[] objects;

    EnumSetHandler(Class type, Object[] objects) {
        this.type = type;
        this.objects = objects;
    }

    @SuppressWarnings("unchecked")
    private Object readResolve() {
        EnumSet enumSet = EnumSet.noneOf(type);
        enumSet.addAll(Arrays.asList(objects));
        return enumSet;
    }
}
