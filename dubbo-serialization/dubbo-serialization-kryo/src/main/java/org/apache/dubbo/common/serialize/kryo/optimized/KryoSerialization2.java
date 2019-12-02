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
package org.apache.dubbo.common.serialize.kryo.optimized;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.apache.dubbo.common.serialize.Constants.KRYO_SERIALIZATION_ID;

/**
 * TODO for now kryo serialization doesn't deny classes that don't implement the serializable interface
 *
 * <pre>
 *     e.g. &lt;dubbo:protocol serialization="kryo" /&gt;
 * </pre>
 */
public class KryoSerialization2 implements Serialization {

    @Override
    public byte getContentTypeId() {
        return KRYO_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "x-application/kryo";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream out) throws IOException {
        return new KryoObjectOutput2(out);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream is) throws IOException {
        return new KryoObjectInput2(is);
    }
}
