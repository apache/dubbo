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
package org.apache.dubbo.common.serialize.protobuf.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.Constants;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>
 * Currently, the Dubbo protocol / framework data, such as attachments, event data, etc.,
 * depends on business layer serialization protocol to do serialization before transmitted.
 * That's a problem when using Protobuf as business serialization protocol, because Protobuf does not support raw java Object types,
 * to solve it, we can use one of the following methods:
 *
 * <ul>
 *     <li>1. Package these data with Protobuf so that they can be serialized.</li>
 *     <li>2. Separate the serialization of Dubbo protocol/framework and the service args (easy to cross-platform, cross-language serialization) to avoid the binding of this part and serialization protocol.</li>
 * </ul>
 *
 * </p>
 */
public class GenericProtobufSerialization implements Serialization {

    @Override
    public byte getContentTypeId() {
        return Constants.PROTOBUF_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream output) {
        return new GenericProtobufObjectOutput(output);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream input) {
        return new GenericProtobufObjectInput(input);
    }
}
