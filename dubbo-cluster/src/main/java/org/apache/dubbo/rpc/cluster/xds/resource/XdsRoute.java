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
package org.apache.dubbo.rpc.cluster.xds.resource;

public class XdsRoute {
    private String name;

    private XdsRouteMatch routeMatch;

    private XdsRouteAction routeAction;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public XdsRouteMatch getRouteMatch() {
        return routeMatch;
    }

    public void setRouteMatch(XdsRouteMatch routeMatch) {
        this.routeMatch = routeMatch;
    }

    public XdsRouteAction getRouteAction() {
        return routeAction;
    }

    public void setRouteAction(XdsRouteAction routeAction) {
        this.routeAction = routeAction;
    }
}
