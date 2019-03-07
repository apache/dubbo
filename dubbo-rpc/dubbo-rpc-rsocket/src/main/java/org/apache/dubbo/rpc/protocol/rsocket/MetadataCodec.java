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
package org.apache.dubbo.rpc.protocol.rsocket;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author sixie.xyn on 2019/1/3.
 */
public class MetadataCodec {

    public static Map<String, Object> decodeMetadata(byte[] bytes) throws IOException {
        return JSON.parseObject(new String(bytes, StandardCharsets.UTF_8), Map.class);
    }

    public static byte[] encodeMetadata(Map<String, Object> metadata) throws IOException {
        String jsonStr = JSON.toJSONString(metadata);
        return jsonStr.getBytes(StandardCharsets.UTF_8);
    }

}
