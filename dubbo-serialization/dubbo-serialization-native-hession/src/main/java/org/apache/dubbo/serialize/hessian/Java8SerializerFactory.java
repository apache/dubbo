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
package org.apache.dubbo.serialize.hessian;


import com.caucho.hessian.io.AbstractSerializerFactory;
import com.caucho.hessian.io.ExtSerializerFactory;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.Serializer;
import org.apache.dubbo.serialize.hessian.serializer.java8.DurationHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.InstantHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.LocalDateHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.LocalDateTimeHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.LocalTimeHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.MonthDayHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.OffsetDateTimeHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.OffsetTimeHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.PeriodHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.YearHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.YearMonthHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.ZoneIdSerializer;
import org.apache.dubbo.serialize.hessian.serializer.java8.ZoneOffsetHandle;
import org.apache.dubbo.serialize.hessian.serializer.java8.ZonedDateTimeHandle;

import static org.apache.dubbo.serialize.hessian.serializer.java8.Java8TimeSerializer.create;


public class Java8SerializerFactory extends ExtSerializerFactory {
    public static final AbstractSerializerFactory INSTANCE = new Java8SerializerFactory();

    private Java8SerializerFactory() {
        if (isJava8()) {
            try {
                this.addSerializer(Class.forName("java.time.LocalTime"), create(LocalTimeHandle.class));
                this.addSerializer(Class.forName("java.time.LocalDate"), create(LocalDateHandle.class));
                this.addSerializer(Class.forName("java.time.LocalDateTime"), create(LocalDateTimeHandle.class));

                this.addSerializer(Class.forName("java.time.Instant"), create(InstantHandle.class));
                this.addSerializer(Class.forName("java.time.Duration"), create(DurationHandle.class));
                this.addSerializer(Class.forName("java.time.Period"), create(PeriodHandle.class));

                this.addSerializer(Class.forName("java.time.Year"), create(YearHandle.class));
                this.addSerializer(Class.forName("java.time.YearMonth"), create(YearMonthHandle.class));
                this.addSerializer(Class.forName("java.time.MonthDay"), create(MonthDayHandle.class));

                this.addSerializer(Class.forName("java.time.OffsetDateTime"), create(OffsetDateTimeHandle.class));
                this.addSerializer(Class.forName("java.time.ZoneOffset"), create(ZoneOffsetHandle.class));
                this.addSerializer(Class.forName("java.time.OffsetTime"), create(OffsetTimeHandle.class));
                this.addSerializer(Class.forName("java.time.ZonedDateTime"), create(ZonedDateTimeHandle.class));
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
    }

    @Override
    public Serializer getSerializer(Class cl) throws HessianProtocolException {
        return isZoneId(cl) ? ZoneIdSerializer.getInstance() : super.getSerializer(cl);
    }

    private static boolean isZoneId(Class cl) {
        try {
            return isJava8() && Class.forName("java.time.ZoneId").isAssignableFrom(cl);
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return false;
    }

    private static boolean isJava8() {
        String javaVersion = System.getProperty("java.specification.version");
        return Double.valueOf(javaVersion) >= 1.8;
    }
}
