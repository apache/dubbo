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

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.message.AbstractMessageDecode;
import org.apache.dubbo.rpc.protocol.rest.message.MediaTypeMatcher;
import org.apache.dubbo.rpc.protocol.rest.util.DataParseUtils;
import org.apache.dubbo.rpc.protocol.rest.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

@Adaptive("text")
public class TextDecode extends AbstractMessageDecode {


    @Override
    public Object decode(InputStream inputStream, Class targetType) throws Exception {
        return DataParseUtils.stringTypeConvert(targetType, StreamUtils.copyToString(inputStream, Charset.defaultCharset()));
    }

    @Override
    public boolean contentTypeSupport(MediaType mediaType) {
        return MediaTypeMatcher.TEXT_PLAIN.mediaSupport(mediaType);
    }
}
