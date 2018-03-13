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
package com.alibaba.dubbo.governance.web.common.resolver;

import com.alibaba.citrus.service.dataresolver.DataResolver;
import com.alibaba.citrus.service.dataresolver.DataResolverContext;
import com.alibaba.citrus.service.dataresolver.DataResolverFactory;
import com.alibaba.citrus.turbine.TurbineRunDataInternal;
import com.alibaba.citrus.turbine.util.TurbineUtil;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * MapDataResolverFactory
 *
 */
public class MapDataResolverFactory implements DataResolverFactory {

    @Autowired
    private HttpServletRequest request;

    public DataResolver getDataResolver(DataResolverContext context) {
        if (Map.class == context.getTypeInfo().getRawType()) {
            return new MapDataResolver(context);
        }
        return null;
    }

    public class MapDataResolver implements DataResolver {

        public final DataResolverContext context;

        public MapDataResolver(DataResolverContext context) {
            this.context = context;
        }

        public Object resolve() {
            TurbineRunDataInternal rundata = (TurbineRunDataInternal) TurbineUtil.getTurbineRunData(request);
            return new ParameterMap(request, rundata.getContext(), rundata);
        }

    }

}
