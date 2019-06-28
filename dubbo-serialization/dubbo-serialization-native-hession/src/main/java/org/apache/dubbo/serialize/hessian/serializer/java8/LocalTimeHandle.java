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

package org.apache.dubbo.serialize.hessian.serializer.java8;


import com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.time.LocalTime;

public class LocalTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -5892919085390462315L;

    private int hour;
    private int minute;
    private int second;
    private int nano;

    public LocalTimeHandle() {
    }

    public LocalTimeHandle(Object o) {
        try {
            LocalTime localTime = (LocalTime) o;
            this.hour = localTime.getHour();
            this.minute = localTime.getMinute();
            this.second = localTime.getSecond();
            this.nano = localTime.getNano();
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            return LocalTime.of(hour, minute, second, nano);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
