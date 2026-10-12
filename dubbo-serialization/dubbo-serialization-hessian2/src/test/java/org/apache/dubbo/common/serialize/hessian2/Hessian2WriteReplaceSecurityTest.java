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
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for https://github.com/apache/dubbo/issues/16287: classes
 * with a writeReplace() method must not bypass the serialization security
 * check enforced by {@link Hessian2SerializerFactory#checkSerializable}.
 */
class Hessian2WriteReplaceSecurityTest {

    /** Non-Serializable class whose writeReplace() returns a legal value. */
    static class NotSerializableWithReplace {
        Object writeReplace() {
            return "replaced";
        }
    }

    /** Serializable class whose writeReplace() returns a non-Serializable holder. */
    static class SerializableWithHolderReplace implements Serializable {
        private static final long serialVersionUID = 1L;

        Object writeReplace() {
            return new SecretHolder("s3cret");
        }
    }

    static final class SecretHolder {
        @SuppressWarnings("unused")
        private final String secret;

        SecretHolder(String secret) {
            this.secret = secret;
        }
    }

    /** Legal case: a Serializable class whose writeReplace() returns a String. */
    static class SerializableWithStringReplace implements Serializable {
        private static final long serialVersionUID = 1L;

        Object writeReplace() {
            return "replaced";
        }
    }

    private ObjectOutput newOutput() throws java.io.IOException {
        FrameworkModel frameworkModel = new FrameworkModel();
        Serialization serialization =
                frameworkModel.getExtensionLoader(Serialization.class).getExtension("hessian2");
        URL url = URL.valueOf("").setScopeModel(frameworkModel);
        return serialization.serialize(url, new ByteArrayOutputStream());
    }

    @Test
    void nonSerializableWithReplaceIsRejected() throws Exception {
        ObjectOutput out = newOutput();
        assertThrows(java.io.IOException.class, () -> {
            out.writeObject(new NotSerializableWithReplace());
            out.flushBuffer();
        });
    }

    @Test
    void nonSerializableReplacementIsRejected() throws Exception {
        ObjectOutput out = newOutput();
        assertThrows(java.io.IOException.class, () -> {
            out.writeObject(new SerializableWithHolderReplace());
            out.flushBuffer();
        });
    }

    @Test
    void serializableWithLegalReplaceStillWorks() throws Exception {
        ObjectOutput out = newOutput();
        out.writeObject(new SerializableWithStringReplace());
        out.flushBuffer();
    }
}
