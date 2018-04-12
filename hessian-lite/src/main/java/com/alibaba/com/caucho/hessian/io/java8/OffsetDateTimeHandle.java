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

package com.alibaba.com.caucho.hessian.io.java8;

import com.alibaba.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class OffsetDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -7823900532640515312L;

    private Object dateTime;
    private Object offset;

    public OffsetDateTimeHandle() {
    }

    public OffsetDateTimeHandle(Object o) {
        try {
            Class c = Class.forName("java.time.OffsetDateTime");
            Method m = c.getDeclaredMethod("toLocalDateTime");
            this.dateTime = m.invoke(o);
            m = c.getDeclaredMethod("getOffset");
            this.offset = m.invoke(o);
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {

        try {
            Class c = Class.forName("java.time.OffsetDateTime");
            Method m = c.getDeclaredMethod("of", Class.forName("java.time.LocalDateTime"),
                    Class.forName("java.time.ZoneOffset"));
            return m.invoke(null, dateTime, offset);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
