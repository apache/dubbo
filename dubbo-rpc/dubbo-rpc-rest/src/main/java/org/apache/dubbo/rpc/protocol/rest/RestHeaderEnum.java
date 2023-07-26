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
package org.apache.dubbo.rpc.protocol.rest;

import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;

public enum RestHeaderEnum {
    CONTENT_TYPE(RestConstant.CONTENT_TYPE),
    ACCEPT(RestConstant.ACCEPT),
    GROUP(RestConstant.REST_HEADER_PREFIX + RestConstant.GROUP),
    VERSION(RestConstant.REST_HEADER_PREFIX + RestConstant.VERSION),
    PATH(RestConstant.REST_HEADER_PREFIX + RestConstant.PATH),
    KEEP_ALIVE_HEADER(RestConstant.KEEP_ALIVE_HEADER),
    CONNECTION(RestConstant.CONNECTION),
    REST_HEADER_PREFIX(RestConstant.REST_HEADER_PREFIX),
    TOKEN_KEY(RestConstant.REST_HEADER_PREFIX + RestConstant.TOKEN_KEY),


    ;
    private final String header;

    RestHeaderEnum(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }
}
