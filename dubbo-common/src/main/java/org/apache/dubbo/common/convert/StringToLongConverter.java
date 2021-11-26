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
package org.apache.dubbo.common.convert;

import static java.lang.Long.valueOf;
import static org.apache.dubbo.common.utils.StringUtils.isNotEmpty;

/**
 * The class to convert {@link String} to {@link Long}
 *
 * @since 2.7.6
 */
public class StringToLongConverter implements StringConverter<Long> {

    @Override
    public Long convert(String source) {
        return isNotEmpty(source) ? valueOf(source) : null;
    }


    @Override
    public int getPriority() {
        return NORMAL_PRIORITY + 1;
    }
}
