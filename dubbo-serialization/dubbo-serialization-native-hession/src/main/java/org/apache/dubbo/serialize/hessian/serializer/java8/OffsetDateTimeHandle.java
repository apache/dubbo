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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class OffsetDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -7823900532640515312L;

    private LocalDateTime dateTime;
    private ZoneOffset offset;

    public OffsetDateTimeHandle() {
    }

    public OffsetDateTimeHandle(Object o) {
        try {
            OffsetDateTime offsetDateTime = (OffsetDateTime) o;
            this.dateTime = offsetDateTime.toLocalDateTime();
            this.offset = offsetDateTime.getOffset();
        } catch (Throwable t) {
            // ignore
        }
    }

    private Object readResolve() {
        try {
            return OffsetDateTime.of(dateTime, offset);
        } catch (Throwable t) {
            // ignore
        }
        return null;
    }
}
