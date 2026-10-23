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
package org.apache.dubbo.common.serialize;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class DefaultSerializationExceptionWrapperTest {

    @Test
    void testCleanupForwardedToObjectInput() throws IOException {
        ObjectInput target = mock(ObjectInput.class, withSettings().extraInterfaces(Cleanable.class));
        Serialization serialization = mock(Serialization.class);
        when(serialization.deserialize(null, null)).thenReturn(target);

        ObjectInput in = new DefaultSerializationExceptionWrapper(serialization).deserialize(null, null);

        Assertions.assertInstanceOf(Cleanable.class, in);
        ((Cleanable) in).cleanup();
        verify((Cleanable) target).cleanup();
    }

    @Test
    void testCleanupForwardedToObjectOutput() throws IOException {
        ObjectOutput target = mock(ObjectOutput.class, withSettings().extraInterfaces(Cleanable.class));
        Serialization serialization = mock(Serialization.class);
        when(serialization.serialize(null, null)).thenReturn(target);

        ObjectOutput out = new DefaultSerializationExceptionWrapper(serialization).serialize(null, null);

        Assertions.assertInstanceOf(Cleanable.class, out);
        ((Cleanable) out).cleanup();
        verify((Cleanable) target).cleanup();
    }

    @Test
    void testCleanupIgnoredWhenTargetNotCleanable() throws IOException {
        Serialization serialization = mock(Serialization.class);
        when(serialization.deserialize(null, null)).thenReturn(mock(ObjectInput.class));
        when(serialization.serialize(null, null)).thenReturn(mock(ObjectOutput.class));
        DefaultSerializationExceptionWrapper wrapper = new DefaultSerializationExceptionWrapper(serialization);

        Assertions.assertDoesNotThrow(() -> ((Cleanable) wrapper.deserialize(null, null)).cleanup());
        Assertions.assertDoesNotThrow(() -> ((Cleanable) wrapper.serialize(null, null)).cleanup());
    }
}
