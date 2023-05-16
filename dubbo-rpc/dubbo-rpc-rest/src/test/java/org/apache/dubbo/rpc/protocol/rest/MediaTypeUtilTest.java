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

import org.apache.dubbo.metadata.rest.media.MediaType;
import org.apache.dubbo.rpc.protocol.rest.exception.UnSupportContentTypeException;
import org.apache.dubbo.rpc.protocol.rest.util.MediaTypeUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MediaTypeUtilTest {

    @Test
    void testException() {

        Assertions.assertThrows(UnSupportContentTypeException.class, () -> {
            MediaTypeUtil.convertMediaType(null, "aaaaa");

        });


    }

    @Test
    void testConvertMediaType() {
        MediaType mediaType = MediaTypeUtil.convertMediaType(null, new String[]{MediaType.APPLICATION_JSON_VALUE.value});

        Assertions.assertEquals(MediaType.APPLICATION_JSON_VALUE, mediaType);


        mediaType = MediaTypeUtil.convertMediaType(int.class, null);

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mediaType);

        mediaType = MediaTypeUtil.convertMediaType(null, new String[]{MediaType.ALL_VALUE.value});

        Assertions.assertEquals(MediaType.APPLICATION_JSON_VALUE, mediaType);

        mediaType = MediaTypeUtil.convertMediaType(String.class, new String[]{MediaType.TEXT_XML.value});

        Assertions.assertEquals(MediaType.TEXT_XML, mediaType);

    }
}
