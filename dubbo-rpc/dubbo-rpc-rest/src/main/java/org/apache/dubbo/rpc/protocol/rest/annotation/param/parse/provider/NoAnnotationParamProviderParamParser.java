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
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

/**
 * body param parse
 * users can custom NoAnnotationParamProviderParamParser
 * and getParamType must return ParamType.PROVIDER_NO_ANNOTATION
 * and order is smaller than NoAnnotationParamProviderParamParser`s order
 */
@Activate(value = RestConstant.PROVIDER_NO_ANNOTATION, order = Integer.MAX_VALUE)
public class NoAnnotationParamProviderParamParser extends ProviderParamParser {

    @Override
    protected void doParse(ProviderParseContext parseContext, ArgInfo argInfo) {

        Object[] arrayArgs = parseContext.getArrayArgs();

        int index = argInfo.getIndex();

        Object arg = arrayArgs[index];

        Object convertArg = JsonUtils.toJavaObject(JsonUtils.toJson(arg), argInfo.actualReflectType());

        parseContext.setValueByIndex(index, convertArg);
    }

    @Override
    public ParamType getParamType() {
        return ParamType.PROVIDER_NO_ANNOTATION;
    }
}
