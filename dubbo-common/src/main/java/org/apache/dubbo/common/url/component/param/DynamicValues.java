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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicValues implements ParamValue {
    private final Map<Integer, String> index2Value = new ConcurrentHashMap<>();
    private final Map<String, Integer> value2Index = new ConcurrentHashMap<>();
    private int indexSeq = 0;

    public DynamicValues(String defaultVal) {
        if (defaultVal == null) {
            indexSeq += 1;
        } else {
            add(defaultVal);
        }
    }

    public int add(String value) {
        Integer index = value2Index.get(value);
        if (index != null) {
            return index;
        } else {
            synchronized (this) {
                // thread safe
                if (!value2Index.containsKey(value)) {
                    value2Index.put(value, indexSeq);
                    index2Value.put(indexSeq, value);
                    indexSeq += 1;
                }
            }
        }
        return value2Index.get(value);
    }

    @Override
    public String getN(Integer n) {
        return index2Value.get(n);
    }

    @Override
    public Integer getIndex(String value) {
        Integer index = value2Index.get(value);
        if (index == null) {
            return add(value);
        }
        return index;
    }

    @Override
    public String defaultVal() {
        return getN(0);
    }
}
