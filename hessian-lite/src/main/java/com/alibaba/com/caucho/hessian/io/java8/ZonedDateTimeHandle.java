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
public class ZonedDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -6933460123278647569L;

    private Object dateTime;
    private Object offset;
    private String zoneId;


    public ZonedDateTimeHandle() {
    }

    public ZonedDateTimeHandle(Object o) {
        try {
            Class c = Class.forName("java.time.ZonedDateTime");
            Method m = c.getDeclaredMethod("toLocalDateTime");
            this.dateTime = m.invoke(o);
            m = c.getDeclaredMethod("getOffset");
            this.offset = m.invoke(o);
            m = c.getDeclaredMethod("getZone");
            Object zone = m.invoke(o);
            if (zone != null) {
                Class zoneId = Class.forName("java.time.ZoneId");
                m = zoneId.getDeclaredMethod("getId");
                this.zoneId = (String) m.invoke(zone);
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            Class zoneDateTime = Class.forName("java.time.ZonedDateTime");
            Method ofLocal = zoneDateTime.getDeclaredMethod("ofLocal", Class.forName("java.time.LocalDateTime"),
                    Class.forName("java.time.ZoneId"), Class.forName("java.time.ZoneOffset"));
            Class c = Class.forName("java.time.ZoneId");
            Method of = c.getDeclaredMethod("of", String.class);
            return ofLocal.invoke(null, dateTime, of.invoke(null, this.zoneId), offset);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
