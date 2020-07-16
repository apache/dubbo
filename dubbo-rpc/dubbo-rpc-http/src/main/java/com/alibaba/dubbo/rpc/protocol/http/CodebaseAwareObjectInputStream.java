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
package com.alibaba.dubbo.rpc.protocol.http;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.SerialDetector;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectStreamClass;

public class CodebaseAwareObjectInputStream extends org.springframework.remoting.rmi.CodebaseAwareObjectInputStream {
    private static final Logger logger = LoggerFactory.getLogger(CodebaseAwareObjectInputStream.class);

    public CodebaseAwareObjectInputStream(InputStream in, ClassLoader classLoader, boolean acceptProxyClasses) throws IOException {
        super(in, classLoader, acceptProxyClasses);
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass serialInput) throws IOException, ClassNotFoundException {
        if (SerialDetector.isClassInBlacklist(serialInput)) {
            if (!SerialDetector.shouldCheck()) {
                // Reporting mode
                logger.info(String.format("Blacklist match: '%s'", serialInput.getName()));
            } else {
                // Blocking mode
                logger.error(String.format("Blocked by blacklist'. Match found for '%s'", serialInput.getName()));
                throw new InvalidClassException(serialInput.getName(), "Class blocked from deserialization (blacklist)");
            }
        }
        return super.resolveClass(serialInput);
    }
}
