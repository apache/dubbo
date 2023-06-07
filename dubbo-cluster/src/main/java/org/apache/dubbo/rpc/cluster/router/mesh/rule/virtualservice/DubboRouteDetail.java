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

package org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice;

import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.destination.DubboRouteDestination;

import java.util.List;


public class DubboRouteDetail {
    private String name;
    private List<DubboMatchRequest> match;
    private List<DubboRouteDestination> route;
    private boolean throwExceptionIfNotMatched = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DubboMatchRequest> getMatch() {
        return match;
    }

    public void setMatch(List<DubboMatchRequest> match) {
        this.match = match;
    }

    public List<DubboRouteDestination> getRoute() {
        return route;
    }

    public void setRoute(List<DubboRouteDestination> route) {
        this.route = route;
    }

    public boolean isThrowExceptionIfNotMatched() {
        return throwExceptionIfNotMatched;
    }

    public void setThrowExceptionIfNotMatched(boolean throwExceptionIfNotMatched) {
        this.throwExceptionIfNotMatched = throwExceptionIfNotMatched;
    }

    @Override
    public String toString() {
        return "DubboRouteDetail{" +
                "name='" + name + '\'' +
                ", match=" + match +
                ", route=" + route +
                ", throwExceptionIfNotMatched=" + throwExceptionIfNotMatched +
                '}';
    }
}
