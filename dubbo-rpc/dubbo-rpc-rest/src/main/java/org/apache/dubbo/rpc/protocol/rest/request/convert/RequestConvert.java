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
package org.apache.dubbo.rpc.protocol.rest.request.convert;

import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.protocol.rest.annotation.consumer.RequestTemplate;
import org.apache.dubbo.rpc.protocol.rest.request.client.ClientFacade;

@SPI
public interface RequestConvert<REQ, RES, CLIENT> extends ClientFacade<REQ, RES> {

    REQ convert(RequestTemplate requestTemplate);

    Object convertResponse(RES response) throws Exception;

    Object request(RequestTemplate requestTemplate) throws RemotingException;

    RequestConvert init(CLIENT restClient, RestMethodMetadata restMethodMetadata);


}
