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

package org.apache.dubbo.rpc.protocol.rest.message.codec;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.HttpMessageCodec;
import org.jboss.resteasy.specimpl.BuiltResponse;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResteasyResponseCodec implements HttpMessageCodec<byte[], OutputStream> {


    @Override
    public boolean contentTypeSupport(MediaType mediaType, Class<?> targetType) {
        return Response.class.equals(targetType);
    }

    @Override
    public boolean typeSupport(Class<?> targetType) {
        return Response.class.isAssignableFrom(targetType);
    }

    @Override
    public MediaType contentType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    public Object decode(byte[] body, Class<?> targetType) throws Exception {
        return new BuiltResponse(){
            protected InputStream getInputStream() {
                return new ByteArrayInputStream(body);
            }

            @Override
            public Object getEntity() {
                return new String(body, StandardCharsets.UTF_8);
            }
        };

    }

    @Override
    public void encode(OutputStream os, Object unSerializedBody, URL url) throws Exception {
        Response response = (Response) unSerializedBody;
        os.write(JsonUtils.toJson(response.getEntity()).getBytes(StandardCharsets.UTF_8));
    }

}
