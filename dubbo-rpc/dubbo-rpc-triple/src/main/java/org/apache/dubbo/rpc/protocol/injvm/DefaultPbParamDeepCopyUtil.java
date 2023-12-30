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
package org.apache.dubbo.rpc.protocol.injvm;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ProtobufUtils;
import org.apache.dubbo.rpc.protocol.tri.SingleProtobufUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_DESERIALIZE;

/**
 * Provides protobuf class deep copy capacity
 */
public class DefaultPbParamDeepCopyUtil extends DefaultParamDeepCopyUtil {

    private static final String NAME = "protobuf";

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultPbParamDeepCopyUtil.class);

    @Override
    public <T> T copy(URL url, Object src, Class<T> targetClass, Type type) {
        if (src != null && ProtobufUtils.isProtobufClass(src.getClass())) {
            try {
                // encode
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                SingleProtobufUtils.serialize(src, outputStream);
                // decode
                ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                return SingleProtobufUtils.deserialize(inputStream, targetClass);
            } catch (Exception e) {
                logger.error(PROTOCOL_ERROR_DESERIALIZE, "", "", "Unable to deep copy parameter to target class.", e);
            }
        }
        if (src != null && src.getClass().equals(targetClass)) {
            return (T) src;
        } else {
            return null;
        }
    }
}
