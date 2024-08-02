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
package org.apache.dubbo.remoting.http12;

import org.apache.dubbo.remoting.http12.message.DefaultHttpResult.Builder;

import java.util.List;
import java.util.Map;

public interface HttpResult<T> {

    int getStatus();

    Map<String, List<String>> getHeaders();

    T getBody();

    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    static <T> HttpResult<T> of(T body) {
        return new Builder<T>().body(body).build();
    }

    static <T> HttpResult<T> of(int status, T body) {
        return new Builder<T>().status(status).body(body).build();
    }

    static <T> HttpResult<T> of(HttpStatus status, T body) {
        return new Builder<T>().status(status).body(body).build();
    }

    static <T> HttpResult<T> status(int status) {
        return new Builder<T>().status(status).build();
    }

    static <T> HttpResult<T> status(HttpStatus status) {
        return new Builder<T>().status(status).build();
    }

    static <T> HttpResult<T> ok() {
        return new Builder<T>().status(HttpStatus.OK).build();
    }

    static <T> HttpResult<T> moved(String url) {
        return new Builder<T>().moved(url).build();
    }

    static <T> HttpResult<T> found(String url) {
        return new Builder<T>().found(url).build();
    }

    static <T> HttpResult<T> badRequest() {
        return new Builder<T>().status(HttpStatus.BAD_REQUEST).build();
    }

    static <T> HttpResult<T> notFound() {
        return new Builder<T>().status(HttpStatus.NOT_FOUND).build();
    }

    static HttpResult<String> error() {
        return new Builder<String>().status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    static HttpResult<String> error(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    static HttpResult<String> error(int status, String message) {
        return new Builder<String>().status(status).body(message).build();
    }

    static HttpResult<String> error(HttpStatus status, String message) {
        return new Builder<String>().status(status).body(message).build();
    }
}
