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
package org.apache.dubbo.common.url.component.param;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * In lower case
 */
public class FixedParamValue implements ParamValue {
    private final String[] values;
    private final Map<String, Integer> val2Index;

    public FixedParamValue(String... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("the array size of values should be larger than 0");
        }
        this.values = values;
        Map<String, Integer> valueMap = new HashMap<>(values.length);
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                valueMap.put(values[i].toLowerCase(Locale.ROOT), i);
            }
        }
        val2Index = Collections.unmodifiableMap(valueMap);
    }

    /**
     * DEFAULT value will be returned if n = 0
     * @param n
     */
    @Override
    public String getN(int n) {
        return values[n];
    }

    @Override
    public int getIndex(String value) {
        Integer offset = val2Index.get(value.toLowerCase(Locale.ROOT));
        if (offset == null) {
            throw new IllegalArgumentException("unrecognized value " + value
                    + " , please check if value is illegal. " +
                    "Permitted values: " + Arrays.asList(values));
        }
        return offset;
    }
}
