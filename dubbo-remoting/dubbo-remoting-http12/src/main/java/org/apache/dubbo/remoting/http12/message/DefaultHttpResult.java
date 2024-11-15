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
package org.apache.dubbo.remoting.http12.message;

import org.apache.dubbo.common.utils.DateUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DefaultHttpResult<T> implements HttpResult<T> {

    private static final long serialVersionUID = 1L;

    private int status;
    private Map<String, List<String>> headers;
    private T body;

    @Override
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    @Override
    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "DefaultHttpResult{" + "status=" + status + ", headers=" + headers + ", body=" + body + '}';
    }

    public static final class Builder<T> {

        private int status;
        private Map<String, List<String>> headers;
        private T body;

        public Builder<T> status(int status) {
            this.status = status;
            return this;
        }

        public Builder<T> status(HttpStatus status) {
            this.status = status.getCode();
            return this;
        }

        public Builder<T> ok() {
            return status(HttpStatus.OK.getCode());
        }

        public Builder<T> moved(String url) {
            return status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaderNames.LOCATION.getName(), url);
        }

        public Builder<T> found(String url) {
            return status(HttpStatus.FOUND).header(HttpHeaderNames.LOCATION.getName(), url);
        }

        public Builder<T> error() {
            return status(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        public Builder<T> headers(Map<String, List<String>> headers) {
            if (headers == null || headers.isEmpty()) {
                return this;
            }
            Map<String, List<String>> hrs = this.headers;
            if (hrs == null) {
                this.headers = new LinkedHashMap<>(headers);
            } else {
                hrs.putAll(headers);
            }
            return this;
        }

        public Builder<T> headers(HttpHeaders headers) {
            if (headers == null || headers.isEmpty()) {
                return this;
            }
            Map<String, List<String>> hrs = this.headers;
            if (hrs == null) {
                this.headers = hrs = new LinkedHashMap<>(headers.size());
            }
            for (Entry<CharSequence, String> entry : headers) {
                CharSequence key = entry.getKey();
                if (HttpHeaderNames.SET_COOKIE.getKey().equals(key)) {
                    hrs.computeIfAbsent(key.toString(), k -> new ArrayList<>(1)).add(entry.getValue());
                } else {
                    hrs.put(key.toString(), Collections.singletonList(entry.getValue()));
                }
            }
            return this;
        }

        public Builder<T> header(String key, List<String> values) {
            headers().put(key, values);
            return this;
        }

        public Builder<T> header(String key, String... values) {
            headers().put(key, Arrays.asList(values));
            return this;
        }

        public Builder<T> header(String key, String value) {
            headers().put(key, Collections.singletonList(value));
            return this;
        }

        public Builder<T> headerIf(String key, String value) {
            return StringUtils.isEmpty(value) ? this : header(key, value);
        }

        public Builder<T> header(String key, Date value) {
            return header(key, DateUtils.formatHeader(value));
        }

        public Builder<T> header(String key, Object value) {
            return header(key, String.valueOf(value));
        }

        public Builder<T> headerIf(String key, Object value) {
            return value == null ? this : header(key, value);
        }

        public Builder<T> addHeader(String key, String value) {
            headers().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            return this;
        }

        public Builder<T> contentType(String value) {
            return headerIf(HttpHeaderNames.CONTENT_TYPE.getName(), value);
        }

        public Builder<T> from(HttpResult<T> result) {
            status = result.getStatus();
            headers = result.getHeaders() == null ? null : new LinkedHashMap<>(result.getHeaders());
            body = result.getBody();
            return this;
        }

        private Map<String, List<String>> headers() {
            Map<String, List<String>> headers = this.headers;
            if (headers == null) {
                this.headers = headers = new LinkedHashMap<>();
            }
            return headers;
        }

        public Builder<T> body(T body) {
            this.body = body;
            return this;
        }

        public DefaultHttpResult<T> build() {
            DefaultHttpResult<T> result = new DefaultHttpResult<>();
            result.setStatus(status);
            result.setHeaders(headers);
            result.setBody(body);
            return result;
        }
    }
}
