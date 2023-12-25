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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import static org.apache.dubbo.common.constants.CommonConstants.PROTOBUF_MESSAGE_CLASS_NAME;

public class ProtobufUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProtobufUtils.class);

    private static Class<?> protobufClss;

    private ProtobufUtils() {}

    static {
        try {
            protobufClss = ClassUtils.forName(PROTOBUF_MESSAGE_CLASS_NAME, ProtobufUtils.class.getClassLoader());
        } catch (Throwable t) {
            logger.info("protobuf's dependency is absent");
        }
    }

    public static boolean isProtobufClass(Class<?> pojoClazz) {
        if (protobufClss != null) {
            return protobufClss.isAssignableFrom(pojoClazz);
        }
        return false;
    }
}
