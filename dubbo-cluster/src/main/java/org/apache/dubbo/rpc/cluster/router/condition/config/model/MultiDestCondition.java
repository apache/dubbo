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
package org.apache.dubbo.rpc.cluster.router.condition.config.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiDestCondition {
    private Map<String, String> from = new HashMap<>();
    private List<Map<String, String>> to = new ArrayList<>();

    public Map<String, String> getFrom() {
        return from;
    }

    public void setFrom(Map<String, String> from) {
        this.from = from;
    }

    public List<Map<String, String>> getTo() {
        return to;
    }

    public void setTo(List<Map<String, String>> to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return "MultiDestCondition{" + "from=" + from + ", to=" + to + '}';
    }
}
