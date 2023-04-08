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
import org.apache.dubbo.rpc.Constants;

public interface RestConstant {
    String VERSION = CommonConstants.VERSION_KEY;
    String GROUP = CommonConstants.GROUP_KEY;
    String PATH = CommonConstants.PATH_KEY;
    String TOKEN_KEY = Constants.TOKEN_KEY;
    String LOCAL_ADDR = "LOCAL_ADDR";
    String REMOTE_ADDR = "REMOTE_ADDR";
    String LOCAL_PORT = "LOCAL_PORT";
    String REMOTE_PORT = "REMOTE_PORT";
    String PROVIDER_BODY_PARSE = "body";
    String PROVIDER_PARAM_PARSE = "param";
    String PROVIDER_HEADER_PARSE = "header";
    String PROVIDER_PATH_PARSE = "path";

    String ADD_MUST_ATTTACHMENT = "must-intercept";
    String RPCCONTEXT_INTERCEPT = "rpc-context";
    String SERIALIZE_INTERCEPT = "serialize";
    String PATH_SEPARATOR = "/";
    String REQUEST_HEADER_INTERCEPT = "header";
    String PATH_INTERCEPT = "path";
    String KEEP_ALIVE_HEADER = "Keep-Alive";
    String CONNECTION = "Connection";
    String CONTENT_TYPE = "Content-Type";
    String TEXT_PLAIN = "text/plain";
    String ACCEPT = "Accept";
    String DEFAULT_ACCEPT = "*/*";
    String REST_HEADER_PREFIX = "#rest#";


    // http
    String MAX_INITIAL_LINE_LENGTH_PARAM = "max.initial.line.length";
    String MAX_HEADER_SIZE_PARAM = "max.header.size";
    String MAX_CHUNK_SIZE_PARAM = "max.chunk.size";
    String MAX_REQUEST_SIZE_PARAM = "max.request.size";
    String IDLE_TIMEOUT_PARAM = "idle.timeout";
    String KEEP_ALIVE_TIMEOUT_PARAM = "keep.alive.timeout";

    int MAX_REQUEST_SIZE = 1024 * 1024 * 10;
    int MAX_INITIAL_LINE_LENGTH = 4096;
    int MAX_HEADER_SIZE = 8192;
    int MAX_CHUNK_SIZE = 8192;
    int IDLE_TIMEOUT = -1;
    int KEEP_ALIVE_TIMEOUT = 60;


}
