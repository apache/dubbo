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
package org.apache.dubbo.rpc.protocol.rest.constans;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.remoting.Constants;

public interface RestConstant {
    String INTERFACE = CommonConstants.INTERFACE_KEY;
    String METHOD = CommonConstants.METHOD_KEY;
    String PARAMETER_TYPES_DESC = CommonConstants.GENERIC_PARAMETER_DESC;
    String VERSION = CommonConstants.VERSION_KEY;
    String GROUP = CommonConstants.GROUP_KEY;
    String PATH = CommonConstants.PATH_KEY;
    String HOST = CommonConstants.HOST_KEY;
    String LOCAL_ADDR = "LOCAL_ADDR";
    String REMOTE_ADDR = "REMOTE_ADDR";
    String LOCAL_PORT = "LOCAL_PORT";
    String REMOTE_PORT = "REMOTE_PORT";
    String SERIALIZATION_KEY = Constants.SERIALIZATION_KEY;
    String PROVIDER_BODY_PARSE = "body";
    String PROVIDER_PARAM_PARSE = "param";
    String PROVIDER_HEADER_PARSE = "header";
    String PROVIDER_PATH_PARSE = "path";
    String PROVIDER_REQUEST_PARSE = "reuqest";
    String DUBBO_ATTACHMENT_HEADER = "Dubbo-Attachments";
    int MAX_HEADER_SIZE = 8 * 1024;

    String ADD_MUST_ATTTACHMENT = "must-intercept";
    String RPCCONTEXT_INTERCEPT = "rpc-context";
    String SERIALIZE_INTERCEPT = "serialize";
    String CONTENT_TYPE_INTERCEPT = "content-type";
    String PATH_SEPARATOR = "/";
    String REQUEST_PARAM_INTERCEPT = "param";
    String REQUEST_HEADER_INTERCEPT = "header";
    String PATH_INTERCEPT = "path";
    String KEEP_ALIVE_HEADER = "Keep-Alive";
    String CONNECTION = "Connection";
    String KEEP_ALIVE = "keep-alive";
    String CONTENT_TYPE = "Content-Type";
    String APPLICATION_JSON_VALUE = "application/json";
    String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";
    String TEXT_PLAIN = "text/plain";
    String ACCEPT = "Accept";
    String DEFAULT_ACCEPT = "*/*";
}
