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
import java.util.Map;

/**
 * Object output interface.
 */
public interface ObjectOutput extends DataOutput {

    /**
     * write object.
     *
     * @param obj object.
     */
    void writeObject(Object obj) throws IOException;

    /**
     * The following methods are customized for the requirement of Dubbo's RPC protocol implementation. Legacy protocol
     * implementation will try to write Map, Throwable and Null value directly to the stream, which does not meet the
     * restrictions of all serialization protocols.
     *
     * <p>
     * See how ProtobufSerialization, KryoSerialization implemented these methods for more details.
     * <p>
     *
     * The binding of RPC protocol and biz serialization protocol is not a good practice. Encoding of RPC protocol
     * should be highly independent and portable, easy to cross platforms and languages, for example, like the http headers,
     * restricting the content of headers / attachments to Ascii strings and uses ISO_8859_1 to encode them.
     * https://tools.ietf.org/html/rfc7540#section-8.1.2
     */
    default void writeThrowable(Object obj) throws IOException {
        writeObject(obj);
    }

    default void writeEvent(Object data) throws IOException {
        writeObject(data);
    }

    default void writeAttachments(Map<String, Object> attachments) throws IOException {
        writeObject(attachments);
    }

}
