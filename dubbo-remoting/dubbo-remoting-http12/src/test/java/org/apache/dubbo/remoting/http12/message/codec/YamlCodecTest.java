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
package org.apache.dubbo.remoting.http12.message.codec;

import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.remoting.http12.exception.DecodeException;
import org.apache.dubbo.remoting.http12.message.HttpMessageCodec;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlCodecTest {

    private HttpMessageCodec codec;

    @BeforeEach
    void setUp() {
        FrameworkModel.destroyAll();
        codec = new YamlCodecFactory().createCodec(null, FrameworkModel.defaultModel(), null);
    }

    @AfterEach
    void tearDown() {
        FrameworkModel.destroyAll();
    }

    @Test
    void testDecodeYamlToPojo() {
        setSerializeCheckStatus(SerializeCheckStatus.WARN);
        String content = "username: JohnDoe\nlocation: New York\n";

        User user = (User) codec.decode(newInputStream(content), User.class);

        Assertions.assertEquals("JohnDoe", user.getUsername());
        Assertions.assertEquals("New York", user.getLocation());
    }

    @Test
    void testDecodeRejectsUnsafeYamlClassTag() {
        setSerializeCheckStatus(SerializeCheckStatus.STRICT);

        String content = "!!java.net.Socket {}";

        Assertions.assertThrows(DecodeException.class, () -> codec.decode(newInputStream(content), Object.class));
    }

    private void setSerializeCheckStatus(SerializeCheckStatus status) {
        FrameworkModel.defaultModel()
                .getBeanFactory()
                .getBean(SerializeSecurityManager.class)
                .setCheckStatus(status);
    }

    private ByteArrayInputStream newInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
