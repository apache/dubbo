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
package org.apache.dubbo.rpc.cluster.configurator.parser.model;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.StringMatch;

public class ParamMatch {
    private String key;
    private StringMatch value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public StringMatch getValue() {
        return value;
    }

    public void setValue(StringMatch value) {
        this.value = value;
    }

    public boolean isMatch(URL url) {
        if (key == null) {
            return false;
        }

        String input = url.getParameter(key);
        return input != null && value.isMatch(input);
    }

    @Override
    public String toString() {
        return "ParamMatch{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            '}';
    }
}
