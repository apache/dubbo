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
package org.apache.dubbo.registry.dns.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ResolveResult {

    private List<String> hostnameList = new LinkedList<>();

    private List<Integer> port = new LinkedList<>();

    public List<String> getHostnameList() {
        return hostnameList;
    }

    public void setHostnameList(List<String> hostnameList) {
        this.hostnameList = hostnameList;
    }

    public List<Integer> getPort() {
        return port;
    }

    public void setPort(List<Integer> port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolveResult that = (ResolveResult) o;
        return Objects.equals(hostnameList, that.hostnameList) &&
                Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostnameList, port);
    }

    @Override
    public String toString() {
        return "ResolveResult{" +
                "hostnameList=" + hostnameList +
                ", port=" + port +
                '}';
    }
}
