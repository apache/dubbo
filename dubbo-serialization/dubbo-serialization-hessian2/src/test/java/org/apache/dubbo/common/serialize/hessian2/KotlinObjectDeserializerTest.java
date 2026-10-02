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
import org.apache.dubbo.common.utils.DefaultSerializeClassChecker;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Consumer;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A Kotlin {@code object} declares a singleton, and the default bean deserializers break that
 * guarantee by allocating a fresh instance. For a plain {@code object}, whose {@code equals} is
 * inherited identity comparison, that also silently changes which branch a Kotlin {@code when}
 * selects.
 */
class KotlinObjectDeserializerTest {

    /** A Kotlin class that is not an object declaration: metadata present, but no INSTANCE field. */
    @kotlin.Metadata
    static final class KotlinStyleClass implements Serializable {
        private static final long serialVersionUID = 1L;
        private String value = "v";
    }

    /** Metadata present and an INSTANCE field, but not of this class's own type. */
    @kotlin.Metadata
    static final class KotlinStyleForeignInstance implements Serializable {
        private static final long serialVersionUID = 1L;
        public static final String INSTANCE = "not-a-self-reference";
        private String value = "v";
    }

    private byte[] write(Consumer<ObjectOutput> body) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutput objectOutput = serialization().serialize(url(), out);
        body.accept(objectOutput);
        objectOutput.flushBuffer();
        return out.toByteArray();
    }

    private ObjectInput read(byte[] bytes) throws IOException {
        return serialization().deserialize(url(), new ByteArrayInputStream(bytes));
    }

    private Object roundTrip(Object value) throws IOException, ClassNotFoundException {
        return read(write(o -> {
                    try {
                        o.writeObject(value);
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }))
                .readObject();
    }

    private Serialization serialization() {
        return new FrameworkModel().getExtensionLoader(Serialization.class).getExtension("hessian2");
    }

    private URL url() {
        return URL.valueOf("").setScopeModel(new FrameworkModel());
    }

    @Test
    void testKotlinObjectKeepsSingletonIdentity() throws Exception {
        Object result = roundTrip(KotlinStyleObject.INSTANCE);

        Assertions.assertSame(
                KotlinStyleObject.INSTANCE,
                result,
                "a Kotlin object must deserialize to its declared INSTANCE, not a new instance");
    }

    /**
     * The singleton is authoritative, as an enum constant is: its serialized field data is read so
     * that the stream stays aligned, then discarded rather than written back over shared state.
     */
    @Test
    void testTransmittedStateIsDiscardedAndStreamStaysAligned() throws Exception {
        KotlinStyleObject.INSTANCE.setState("written-to-the-wire");
        byte[] bytes = write(out -> {
            try {
                out.writeObject(KotlinStyleObject.INSTANCE);
                out.writeUTF("sentinel");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        KotlinStyleObject.INSTANCE.setState("changed-after-writing");

        ObjectInput in = read(bytes);
        Object first = in.readObject();

        Assertions.assertSame(KotlinStyleObject.INSTANCE, first);
        Assertions.assertEquals(
                "changed-after-writing",
                KotlinStyleObject.INSTANCE.getState(),
                "the singleton's own state must win; transmitted state is discarded");
        Assertions.assertEquals(
                "sentinel",
                in.readUTF(),
                "the serialized fields must still be consumed, or the next value in the stream desyncs");
    }

    /**
     * A peer may send the map encoding rather than the field encoding — Hessian2Input dispatches
     * those to readMap, and the inherited implementation rejects them. The singleton must be
     * resolved there too.
     */
    @Test
    void testKotlinObjectViaMapEncoding() throws Exception {
        Hessian2SerializerFactory factory =
                new Hessian2SerializerFactory(getClass().getClassLoader(), DefaultSerializeClassChecker.getInstance());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bytes);
        out.setSerializerFactory(factory);
        out.writeMapBegin(KotlinStyleObject.class.getName());
        out.writeString("state");
        out.writeString("written-to-the-wire");
        out.writeMapEnd();
        out.flush();

        Hessian2Input in = new Hessian2Input(new ByteArrayInputStream(bytes.toByteArray()));
        in.setSerializerFactory(factory);

        Assertions.assertSame(KotlinStyleObject.INSTANCE, in.readObject());
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

    /**
     * Kotlin emits its metadata annotation on every class, not only on object declarations, so the
     * shape of the INSTANCE field decides. Neither of these is a singleton.
     */
    @Test
    void testKotlinClassesThatAreNotObjectDeclarations() throws Exception {
        for (Object value : Arrays.asList(new KotlinStyleClass(), new KotlinStyleForeignInstance())) {
            Object result = roundTrip(value);

            Assertions.assertInstanceOf(value.getClass(), result);
            Assertions.assertNotSame(value, result, value.getClass().getSimpleName() + " is not an object declaration");
        }
    }
}
