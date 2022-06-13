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
package org.apache.dubbo.common.url.component;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Act like URLParam, will not use DynamicParamTable to compress parameters,
 * which can support serializer serialization and deserialization.
 * DynamicParamTable is environment hard related.
 */
public class URLPlainParam extends URLParam implements Serializable {

    private static final long serialVersionUID = 4722019979665434393L;

    protected URLPlainParam(BitSet key, int[] value, Map<String, String> extraParams, Map<String, Map<String, String>> methodParameters, String rawParam) {
        super(key, value, extraParams, methodParameters, rawParam);
        this.enableCompressed = false;
    }

    public static URLPlainParam toURLPlainParam(URLParam urlParam) {
        Map<String, String> params = Collections.unmodifiableMap(new HashMap<>(urlParam.getParameters()));
        return new URLPlainParam(new BitSet(), new int[0], params, urlParam.getMethodParameters(), urlParam.getRawParam());
    }

}
