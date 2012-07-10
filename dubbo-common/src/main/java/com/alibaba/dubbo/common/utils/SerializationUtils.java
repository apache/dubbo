/*
 * Copyright 1999-2011 Alibaba Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.dubbo.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;

/**
 * @author <a href="mailto:gang.lvg@alibaba-inc.com">kimi</a>
 */
public class SerializationUtils {

    private static final Logger log = LoggerFactory.getLogger(SerializationUtils.class);

    private SerializationUtils() {}

    public static Object javaDeserialize(byte[] bytes) throws Exception {
            ObjectInputStream objectInputStream = new ObjectInputStream(
                new ByteArrayInputStream(bytes));
            try {
                return objectInputStream.readObject();
            } finally {
                close(objectInputStream);
            }
        }

    public static byte[] javaSerialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);
        try {
            objectOutputStream.writeObject(obj);
            return out.toByteArray();
        } finally {
            close(objectOutputStream);
        }
    }

    private static void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("Close closeable failed: " + e.getMessage(), e);
            }
        }
    }

}
