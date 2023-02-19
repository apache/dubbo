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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.rest.ArgInfo;
import org.apache.dubbo.metadata.rest.ParamType;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodecManager;
import org.apache.dubbo.rpc.protocol.rest.request.RequestFacade;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;

import java.io.InputStream;


/**
 * body param parse
 */
@Activate(value = RestConstant.PROVIDER_BODY_PARSE)
public class BodyProviderParamParser extends ProviderParamParser {
    private static final Logger logger = LoggerFactory.getLogger(BodyProviderParamParser.class);

    @Override
    protected void doParse(ProviderParseContext parseContext, ArgInfo argInfo) {

        RequestFacade request = parseContext.getRequestFacade();

        try {
            String contentType = parseContext.getRequestFacade().getHeader(RestConstant.CONTENT_TYPE);
            //TODO MAP<String,String> convert
            // TODO  url builder

            // TODO json utils no stream convert
            // TODO add form parse
            InputStream inputStream = request.getInputStream();
            MediaType mediaType = MediaTypeUtil.convertMediaType(contentType);
            Object param = HttpMessageCodecManager.httpMessageDecode(inputStream, argInfo.getParamType(), mediaType);
            parseContext.setValueByIndex(argInfo.getIndex(), param);
        } catch (Exception e) {
            logger.error("BodyProviderParamParser parse error: {}", e);
        }
    }

    @Override
    protected ParamType getParamType() {
        return ParamType.BODY;
    }
}
