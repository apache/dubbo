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
package org.apache.dubbo.common.serialize.fastjson2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.rpc.model.FrameworkModel;

import static org.apache.dubbo.common.serialize.Constants.FASTJSON2_SERIALIZATION_ID;

/**
 * FastJson serialization implementation
 *
 * <pre>
 *     e.g. &lt;dubbo:protocol serialization="fastjson" /&gt;
 * </pre>
 */
public class FastJson2Serialization implements Serialization {

    @Override
    public byte getContentTypeId() {
        return FASTJSON2_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "text/jsonb";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
        Fastjson2CreatorManager fastjson2CreatorManager = Optional.ofNullable(url)
            .map(URL::getOrDefaultFrameworkModel)
            .orElse(FrameworkModel.defaultModel())
            .getBeanFactory().getBean(Fastjson2CreatorManager.class);
        Fastjson2SecurityManager fastjson2SecurityManager = Optional.ofNullable(url)
            .map(URL::getOrDefaultFrameworkModel)
            .orElse(FrameworkModel.defaultModel())
            .getBeanFactory().getBean(Fastjson2SecurityManager.class);

        return new FastJson2ObjectOutput(fastjson2CreatorManager, fastjson2SecurityManager, output);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) throws IOException {
        Fastjson2CreatorManager fastjson2CreatorManager = Optional.ofNullable(url)
            .map(URL::getOrDefaultFrameworkModel)
            .orElse(FrameworkModel.defaultModel())
            .getBeanFactory().getBean(Fastjson2CreatorManager.class);
        Fastjson2SecurityManager fastjson2SecurityManager = Optional.ofNullable(url)
            .map(URL::getOrDefaultFrameworkModel)
            .orElse(FrameworkModel.defaultModel())
            .getBeanFactory().getBean(Fastjson2SecurityManager.class);

        return new FastJson2ObjectInput(fastjson2CreatorManager, fastjson2SecurityManager, input);
    }

}
