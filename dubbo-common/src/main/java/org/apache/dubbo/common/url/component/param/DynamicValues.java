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
    private volatile String[] index2Value = new String[1];
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
                    if (indexSeq == Integer.MAX_VALUE) {
                        throw new IllegalStateException("URL Param Cache is full.");
                    }
                    // copy on write, only support append now
                    String[] newValues = new String[indexSeq + 1];
                    System.arraycopy(index2Value, 0, newValues, 0, indexSeq);
                    newValues[indexSeq] = value;
                    index2Value = newValues;
                    value2Index.put(value, indexSeq);
                    indexSeq += 1;
                }
            }
        }
        return value2Index.get(value);
    }

    @Override
    public String getN(int n) {
        if (n == -1) {
            return null;
        }
        return index2Value[n];
    }

    @Override
    public int getIndex(String value) {
        if (value == null) {
            return -1;
        }
        Integer index = value2Index.get(value);
        if (index == null) {
            return add(value);
        }
        return index;
    }
}
