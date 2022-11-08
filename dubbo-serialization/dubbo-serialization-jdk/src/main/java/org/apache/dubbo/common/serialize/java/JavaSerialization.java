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
package org.apache.dubbo.common.serialize.java;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNSAFE_SERIALIZATION;
import static org.apache.dubbo.common.serialize.Constants.JAVA_SERIALIZATION_ID;

/**
 * Java serialization implementation
 *
 * <pre>
 *     e.g. &lt;dubbo:protocol serialization="java" /&gt;
 * </pre>
 */
public class JavaSerialization implements Serialization {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JavaSerialization.class);
    private static final AtomicBoolean warn = new AtomicBoolean(false);

    @Override
    public byte getContentTypeId() {
        return JAVA_SERIALIZATION_ID;
    }

    @Override
    public String getContentType() {
        return "x-application/java";
    }

    @Override
    public ObjectOutput serialize(URL url, OutputStream out) throws IOException {
        if (warn.compareAndSet(false, true)) {
            logger.error(PROTOCOL_UNSAFE_SERIALIZATION, "", "", "Java serialization is unsafe. Dubbo Team do not recommend anyone to use it." +
                "If you still want to use it, please follow [JEP 290](https://openjdk.java.net/jeps/290)" +
                "to set serialization filter to prevent deserialization leak.");
        }
        return new JavaObjectOutput(out);
    }

    @Override
    public ObjectInput deserialize(URL url, InputStream is) throws IOException {
        if (warn.compareAndSet(false, true)) {
            logger.error(PROTOCOL_UNSAFE_SERIALIZATION, "", "", "Java serialization is unsafe. Dubbo Team do not recommend anyone to use it." +
                "If you still want to use it, please follow [JEP 290](https://openjdk.java.net/jeps/290)" +
                "to set serialization filter to prevent deserialization leak.");
        }
        return new JavaObjectInput(is);
    }

}
