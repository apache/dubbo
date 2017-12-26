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
package com.alibaba.dubbo.governance.web.common.pulltool;

import com.alibaba.citrus.service.pull.ToolFactory;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.RouteService;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * PullTool for accessing message bundle.
 *
 */
public class ToolUtil implements ToolFactory {
    @Autowired
    OverrideService overrideService;

    @Autowired
    RouteService routeService;
    private boolean singleton = false;

    public Object createTool() throws Exception {
        Tool tool = new Tool();
        tool.setOverrideService(overrideService);
        tool.setRouteService(routeService);
        return tool;
    }

    public boolean isSingleton() {
        return this.singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

}
