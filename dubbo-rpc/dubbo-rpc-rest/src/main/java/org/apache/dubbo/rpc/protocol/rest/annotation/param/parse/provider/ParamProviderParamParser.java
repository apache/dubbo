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
package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider;


import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Http Parameter param parse
 */
@Activate(value = RestConstant.PROVIDER_PARAM_PARSE)
public class ParamProviderParamParser extends ProviderParamParser {
    @Override
    protected void doParse(ProviderParseContext parseContext, ArgInfo argInfo) {

        //TODO MAP<String,String> convert
        RequestFacade request = parseContext.getRequestFacade();

        if (Map.class.isAssignableFrom(argInfo.getParamType())) {

            Map<String, String> paramMap = new LinkedHashMap<>();
            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                paramMap.put(name, request.getParameter(name));
            }
            parseContext.setValueByIndex(argInfo.getIndex(), paramMap);
            return;

        }

        String param = request.getParameter(argInfo.getAnnotationNameAttribute());

        Object paramValue = paramTypeConvert(argInfo.getParamType(), param);
        parseContext.setValueByIndex(argInfo.getIndex(), paramValue);
    }

    @Override
    protected ParamType getParamType() {
        return ParamType.PARAM;
    }
}
