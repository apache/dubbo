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
package org.apache.dubbo.mcp.server.demo.demo;

import java.util.List;
import java.util.Map;

public class ComplexRequest {
    private String greeting;
    private int count;
    private boolean active;
    private NestedDetail nestedDetail;
    private List<String> tags;
    private Map<String, String> attributes;

    // Default constructor (important for deserialization)
    public ComplexRequest() {}

    // Getters and Setters
    public String getGreeting() {
        return greeting;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public NestedDetail getNestedDetail() {
        return nestedDetail;
    }

    public void setNestedDetail(NestedDetail nestedDetail) {
        this.nestedDetail = nestedDetail;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "ComplexRequest{" + "greeting='"
                + greeting + '\'' + ", count="
                + count + ", active="
                + active + ", nestedDetail="
                + nestedDetail + ", tags="
                + tags + ", attributes="
                + attributes + '}';
    }
}
