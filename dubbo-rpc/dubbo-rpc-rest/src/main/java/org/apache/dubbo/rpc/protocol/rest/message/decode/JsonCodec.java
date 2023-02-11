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
package org.apache.dubbo.rpc.protocol.rest.message.decode;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.json.JSON;
import org.apache.dubbo.common.json.factory.JsonFactory;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.AbstractMessageCodec;
import org.apache.dubbo.rpc.protocol.rest.message.MediaTypeMatcher;
import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Activate("json")
public class JsonCodec extends AbstractMessageCodec {


    @Override
    public Object decode(InputStream inputStream, Class targetType) throws Exception {
        return DataParseUtils.jsonConvert(targetType, inputStream);
    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType) {
        return MediaTypeMatcher.APPLICATION_JSON.mediaSupport(mediaType);
    }


    @Override
    public void encode(ByteArrayOutputStream outputStream, Object unSerializedBody, URL url) throws Exception {
        JsonFactory jsonFactory = url.getApplicationModel().getAdaptiveExtension(JsonFactory.class);
        JSON json = jsonFactory.createJson(url);
        outputStream.write(json.toJson(unSerializedBody).getBytes(StandardCharsets.UTF_8));
    }
}
