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
package org.apache.dubbo.demo.protobuf.provider;

import org.apache.dubbo.demo.protobuf.api.GooglePb.GooglePBRequestType;
import org.apache.dubbo.demo.protobuf.api.GooglePb.GooglePBResponseType;
import org.apache.dubbo.demo.protobuf.api.ProtobufDemoService;

/**
 * 2019/4/8
 */
public class ProtobufDemoServiceImpl implements ProtobufDemoService {
  @Override
  public GooglePBResponseType sayHello(GooglePBRequestType request) {
    GooglePBResponseType response = GooglePBResponseType.newBuilder().setResponse("message from server :"+request.getReq()).build();
    return response;
  }
}
