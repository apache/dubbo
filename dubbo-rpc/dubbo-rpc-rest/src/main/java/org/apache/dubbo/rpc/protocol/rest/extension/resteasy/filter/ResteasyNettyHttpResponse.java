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
package org.apache.dubbo.rpc.protocol.rest.extension.resteasy.filter;

import org.apache.dubbo.rpc.protocol.rest.netty.NettyHttpResponse;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpResponse;

public class ResteasyNettyHttpResponse implements HttpResponse {

    private NettyHttpResponse response;

    private MultivaluedMap<String, Object> multivaluedMap = new MultivaluedMapImpl<>();

    public ResteasyNettyHttpResponse(NettyHttpResponse response) {
        this.response = response;
        Map<String, List<String>> outputHeaders = response.getOutputHeaders();

        for (Map.Entry<String, List<String>> headers : outputHeaders.entrySet()) {
            String key = headers.getKey();
            List<String> value = headers.getValue();

            if (value == null || value.isEmpty()) {
                continue;
            }

            for (String val : value) {
                multivaluedMap.add(key, val);
            }
        }
    }

    @Override
    public int getStatus() {
        return response.getStatus();
    }

    @Override
    public void setStatus(int status) {

        response.setStatus(status);
    }

    @Override
    public MultivaluedMap<String, Object> getOutputHeaders() {
        return multivaluedMap;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public void setOutputStream(OutputStream os) {
        response.setOutputStream(os);
    }

    @Override
    public void addNewCookie(NewCookie cookie) {}

    @Override
    public void sendError(int status) throws IOException {

        response.sendError(status);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        response.sendError(status, message);
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {

        response.reset();
    }

    @Override
    public void flushBuffer() throws IOException {}
}
