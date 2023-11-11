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
package org.apache.dubbo.rpc.protocol.rest.util;

import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.annotation.param.parse.provider.ProviderParseContext;
import org.apache.dubbo.rpc.protocol.rest.exception.ParamParseException;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;

public class NoAnnotationBodyParseUtil {

    public static Object[] doParse(ProviderParseContext parseContext) {
        RequestFacade request = parseContext.getRequestFacade();
        try {
            Class<?> objectArraysType = Object[].class;
            MediaType mediaType =
                    MediaTypeUtil.convertMediaType(objectArraysType, MediaType.APPLICATION_JSON_VALUE.value);
            Object[] params = (Object[]) HttpMessageCodecManager.httpMessageDecode(
                    request.getInputStream(), objectArraysType, objectArraysType, mediaType);
            return params;
        } catch (Throwable e) {
            throw new ParamParseException("dubbo rest protocol provider body param parser  error: " + e.getMessage());
        }
    }
}
