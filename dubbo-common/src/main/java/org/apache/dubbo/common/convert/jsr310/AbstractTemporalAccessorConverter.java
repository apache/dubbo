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


import org.apache.dubbo.common.utils.CollectionUtils;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

public abstract class AbstractTemporalAccessorConverter {

    private final List<DateTimeFormatter> dateTimeFormatterList;

    public AbstractTemporalAccessorConverter() {
        this.dateTimeFormatterList = Collections.emptyList();
    }

    public AbstractTemporalAccessorConverter(List<DateTimeFormatter> dateTimeFormatterList) {
        this.dateTimeFormatterList = dateTimeFormatterList;
    }

    public Object generalize(TemporalAccessor accessor) {
        if (CollectionUtils.isNotEmpty(dateTimeFormatterList)) {
            for (DateTimeFormatter dateTimeFormatter : dateTimeFormatterList) {
                try {
                    return dateTimeFormatter.format(accessor);
                } catch (Exception ignore) {
                }
            }
        }
        return accessor.toString();
    }

    public <T extends TemporalAccessor> T realize(Object obj, BiFunction<String, DateTimeFormatter, T> parse, DateTimeFormatter defaultDateTimeFormatter) {
        String text = obj.toString();
        if (CollectionUtils.isNotEmpty(dateTimeFormatterList)) {
            for (DateTimeFormatter dateTimeFormatter : dateTimeFormatterList) {
                try {
                    return parse.apply(text, dateTimeFormatter);
                } catch (Exception ignore) {
                }
            }
        }
        return parse.apply(text, defaultDateTimeFormatter);
    }

}
