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
package org.apache.dubbo.common.serialize.hessian2;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A Kotlin {@code object} declares a singleton, and the default bean deserializers break that
 * guarantee by allocating a fresh instance. For a plain {@code object}, whose {@code equals} is
 * inherited identity comparison, that also silently changes which branch a Kotlin {@code when}
 * selects.
 */
class KotlinObjectDeserializerTest {

    private Object roundTrip(Object value) throws IOException, ClassNotFoundException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization.serialize(url, outputStream);
        objectOutput.writeObject(value);
        objectOutput.flushBuffer();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        ObjectInput objectInput = serialization.deserialize(url, inputStream);
        return objectInput.readObject();
    }

    @Test
    void testKotlinObjectKeepsSingletonIdentity() throws Exception {
        Object result = roundTrip(KotlinStyleObject.INSTANCE);

        Assertions.assertSame(
                KotlinStyleObject.INSTANCE,
                result,
                "a Kotlin object must deserialize to its declared INSTANCE, not a new instance");
    }

    @Test
    void testJavaSingletonBehaviourIsUnchanged() throws Exception {
        Object result = roundTrip(JavaStyleSingleton.INSTANCE);

        Assertions.assertInstanceOf(JavaStyleSingleton.class, result);
        Assertions.assertNotSame(
                JavaStyleSingleton.INSTANCE,
                result,
                "an ordinary Java singleton must keep deserializing to a new instance; "
                        + "the Kotlin handling is gated on kotlin.Metadata to avoid changing this");
    }
}
