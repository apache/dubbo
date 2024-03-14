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

import org.apache.dubbo.common.utils.StringUtils;

public class ResponseMeta {

    private final Integer status;
    private final String reason;

    public ResponseMeta(Integer status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public Integer getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public static ResponseMeta combine(ResponseMeta self, ResponseMeta other) {
        if (self == null) {
            return other;
        }
        if (other == null) {
            return self;
        }
        Integer status = other.getStatus() == null ? self.status : other.getStatus();
        String reason = other.getReason() == null ? self.reason : other.getReason();
        return new ResponseMeta(status, reason);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResponseMeta{");
        if (status != null) {
            sb.append("status=").append(status);
        }
        if (StringUtils.isNotEmpty(reason)) {
            sb.append(", reason='").append(reason).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}
