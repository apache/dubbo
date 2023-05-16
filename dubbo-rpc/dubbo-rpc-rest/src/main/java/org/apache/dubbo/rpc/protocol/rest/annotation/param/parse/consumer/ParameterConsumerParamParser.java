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
package org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.consumer;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;
import org.apache.dubbo.remoting.http.RequestTemplate;

import java.util.List;
import java.util.Map;

@Activate("consumer-parameter")
public class ParameterConsumerParamParser implements BaseConsumerParamParser {
    @Override
    public void parse(ConsumerParseContext parseContext, ArgInfo argInfo) {
        List<Object> args = parseContext.getArgs();

        RequestTemplate requestTemplate = parseContext.getRequestTemplate();

        Object paramValue = args.get(argInfo.getIndex());

        if (paramValue == null) {
            return;
        }


        if (Map.class.isAssignableFrom(argInfo.getParamType())) {
            Map paramValues = (Map) paramValue;
            for (Object name : paramValues.keySet()) {
                requestTemplate.addParam(String.valueOf(name), paramValues.get(name));
            }
        } else {
            requestTemplate.addParam(argInfo.getAnnotationNameAttribute(), paramValue);


        }

    }

    @Override
    public boolean paramTypeMatch(ArgInfo argInfo) {
        return ParamType.PARAM.supportAnno(argInfo.getParamAnnotationType());
    }
}
