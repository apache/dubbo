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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DefaultLocalDateConverter extends AbstractTemporalAccessorConverter implements LocalDateConverter {

    public DefaultLocalDateConverter() {}

    public DefaultLocalDateConverter(List<DateTimeFormatter> dateTimeFormatterSet) {
        super(dateTimeFormatterSet);
    }

    @Override
    public Object generalize(LocalDate localDate) {
        return super.generalize(localDate);
    }

    @Override
    public LocalDate realize(Object obj) {
        return super.realize(obj, LocalDate::parse, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
