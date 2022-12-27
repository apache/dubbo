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
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.AddressMatch;
import org.apache.dubbo.rpc.cluster.router.mesh.rule.virtualservice.match.ListStringMatch;

import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;

public class ConditionMatch {
    private AddressMatch address;
    private ListStringMatch service;
    private ListStringMatch app;
    private List<ParamMatch> param;

    public AddressMatch getAddress() {
        return address;
    }

    public void setAddress(AddressMatch address) {
        this.address = address;
    }

    public ListStringMatch getService() {
        return service;
    }

    public void setService(ListStringMatch service) {
        this.service = service;
    }

    public ListStringMatch getApp() {
        return app;
    }

    public void setApp(ListStringMatch app) {
        this.app = app;
    }

    public List<ParamMatch> getParam() {
        return param;
    }

    public void setParam(List<ParamMatch> param) {
        this.param = param;
    }

    public boolean isMatch(URL url) {
        if (getAddress() != null && !getAddress().isMatch(url.getAddress())) {
            return false;
        }

        if (getService() != null && !getService().isMatch(url.getServiceKey())) {
            return false;
        }

        if (getApp() != null && !getApp().isMatch(url.getParameter(APPLICATION_KEY))) {
            return false;
        }

        if (getParam() != null) {
            for (ParamMatch match : param) {
                if (!match.isMatch(url)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "ConditionMatch{" +
            "address='" + address + '\'' +
            ", service='" + service + '\'' +
            ", app='" + app + '\'' +
            ", param='" + param + '\'' +
            '}';
    }
}
