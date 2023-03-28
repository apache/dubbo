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
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Activate("consumer-form")
public class FormConsumerParamParser implements BaseConsumerParamParser {
    @Override
    public void parse(ConsumerParseContext parseContext, ArgInfo argInfo) {


        List<Object> args = parseContext.getArgs();

        RequestTemplate requestTemplate = parseContext.getRequestTemplate();
        Object value = args.get(argInfo.getIndex());

        if (value == null) {
            return;
        }


        Map<String, List<String>> tmp = new HashMap<>();
        if (DataParseUtils.isTextType(value.getClass())) {
            tmp.put(argInfo.getAnnotationNameAttribute(), Arrays.asList(String.valueOf(value)));
            requestTemplate.body(tmp, Map.class);
        } else if (value instanceof Map) {
            requestTemplate.body(value, Map.class);
        } else {
            Set<String> allFieldNames = ReflectUtils.getAllFieldNames(value.getClass());

            allFieldNames.stream().forEach(entry -> {

                    Object fieldValue = ReflectUtils.getFieldValue(value, entry);
                    tmp.put(String.valueOf(entry), Arrays.asList(String.valueOf(fieldValue)));
                }
            );

            requestTemplate.body(tmp, Map.class);
        }


    }

    @Override
    public boolean paramTypeMatch(ArgInfo argInfo) {
        return ParamType.FORM.supportAnno(argInfo.getParamAnnotationType());
    }
}
