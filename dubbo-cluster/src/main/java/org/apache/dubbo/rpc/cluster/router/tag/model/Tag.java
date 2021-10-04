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
package org.apache.dubbo.rpc.cluster.router.tag.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class Tag {
    private String name;
    private List<String> addresses;

    @SuppressWarnings("unchecked")
    public static Tag parseFromMap(Map<String, Object> map) {
        Tag tag = new Tag();
        tag.setName((String) map.get("name"));

        Object addresses = map.get("addresses");
        if (addresses != null && List.class.isAssignableFrom(addresses.getClass())) {
            tag.setAddresses(((List<Object>) addresses).stream().map(String::valueOf).collect(Collectors.toList()));
        }

        return tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }
}
