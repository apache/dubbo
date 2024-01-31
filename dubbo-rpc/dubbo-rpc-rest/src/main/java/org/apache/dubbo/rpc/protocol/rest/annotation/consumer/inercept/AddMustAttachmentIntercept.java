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
package org.apache.dubbo.rpc.protocol.rest.annotation.consumer.inercept;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.remoting.http.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.RestHeaderEnum;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionCreateContext;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.HttpConnectionPreBuildIntercept;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

/**
 * add some must attachment
 */
@Activate(value = RestConstant.ADD_MUST_ATTTACHMENT, order = 1)
public class AddMustAttachmentIntercept implements HttpConnectionPreBuildIntercept {

    @Override
    public void intercept(HttpConnectionCreateContext connectionCreateContext) {

        RequestTemplate requestTemplate = connectionCreateContext.getRequestTemplate();
        ServiceRestMetadata serviceRestMetadata = connectionCreateContext.getServiceRestMetadata();

        requestTemplate.addHeader(RestHeaderEnum.GROUP.getHeader(), serviceRestMetadata.getGroup());
        requestTemplate.addHeader(RestHeaderEnum.VERSION.getHeader(), serviceRestMetadata.getVersion());
        requestTemplate.addHeader(RestHeaderEnum.PATH.getHeader(), serviceRestMetadata.getServiceInterface());
        requestTemplate.addHeader(
                RestHeaderEnum.TOKEN_KEY.getHeader(),
                connectionCreateContext.getUrl().getParameter(RestConstant.TOKEN_KEY));
    }
}
