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
package org.apache.dubbo.common.convert.jsr310;


import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DefaultLocalTimeConverter extends AbstractTemporalAccessorConverter implements LocalTimeConverter {

    public DefaultLocalTimeConverter() {
    }

    public DefaultLocalTimeConverter(List<DateTimeFormatter> dateTimeFormatterSet) {
        super(dateTimeFormatterSet);
    }

    @Override
    public Object generalize(LocalTime localTime) {
        return super.generalize(localTime);
    }

    @Override
    public LocalTime realize(Object obj) {
        return super.realize(obj, LocalTime::parse, DateTimeFormatter.ISO_LOCAL_TIME);
    }

}
