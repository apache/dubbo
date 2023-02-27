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

import org.apache.dubbo.common.utils.StringUtils;

/**
 * The class to convert {@link String} to {@link Byte}
 *
 * @since 3.0.4
 */
public class StringToByteConverter implements StringConverter<Byte> {
    @Override
    public Byte convert(String source) {
        return StringUtils.isNotEmpty(source) ? Byte.valueOf(source) : null;
    }

    @Override
    public int getPriority() {
        return NORMAL_PRIORITY + 9;
    }
}
