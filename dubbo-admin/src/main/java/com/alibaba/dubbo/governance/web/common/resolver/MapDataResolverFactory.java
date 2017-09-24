/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
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
 * @author william.liangf
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
