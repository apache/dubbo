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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResponseMeta {

    private final Integer status;
    private final String reason;
    private final Map<String, List<String>> headers;

    public ResponseMeta(Integer status, String reason, Map<String, List<String>> headers) {
        this.status = status;
        this.reason = reason;
        this.headers = headers;
    }

    public Integer getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public static ResponseMeta combine(ResponseMeta self, ResponseMeta other) {
        if (self == null) {
            return other;
        }
        if (other == null) {
            return self;
        }
        Integer status = other.getStatus() != null ? other.getStatus() : self.status;
        String reason = other.getReason() != null ? other.getReason() : self.reason;
        Map<String, List<String>> headers = self.headers;
        if (other.getHeaders() != null) {
            if (headers != null) {
                headers = new LinkedHashMap<>(headers);
                headers.putAll(other.getHeaders());
            } else {
                headers = other.getHeaders();
            }
        }
        return new ResponseMeta(status, reason, headers);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResponseMeta{");
        if (status != null) {
            sb.append("status=").append(status);
        }
        if (reason != null) {
            sb.append(", reason='").append(reason).append('\'');
        }
        if (headers != null) {
            sb.append(", headers=").append(headers);
        }
        sb.append('}');
        return sb.toString();
    }
}
