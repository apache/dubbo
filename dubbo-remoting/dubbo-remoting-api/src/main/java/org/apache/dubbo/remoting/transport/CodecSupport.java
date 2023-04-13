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

package org.apache.dubbo.remoting.transport;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.remoting.utils.UrlUtils;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_SERIALIZATION;

public class CodecSupport {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(CodecSupport.class);
    private static Map<Byte, Serialization> ID_SERIALIZATION_MAP = new HashMap<Byte, Serialization>();
    private static Map<Byte, String> ID_SERIALIZATIONNAME_MAP = new HashMap<Byte, String>();
    private static Map<String, Byte> SERIALIZATIONNAME_ID_MAP = new HashMap<String, Byte>();
    // Cache null object serialize results, for heartbeat request/response serialize use.
    private static ConcurrentMap<Byte, byte[]> ID_NULLBYTES_MAP = new ConcurrentHashMap<>();

    private static final ThreadLocal<byte[]> TL_BUFFER = ThreadLocal.withInitial(() -> new byte[1024]);

    static {
        ExtensionLoader<Serialization> extensionLoader = FrameworkModel.defaultModel().getExtensionLoader(Serialization.class);
        Set<String> supportedExtensions = extensionLoader.getSupportedExtensions();
        for (String name : supportedExtensions) {
            Serialization serialization = extensionLoader.getExtension(name);
            byte idByte = serialization.getContentTypeId();
            if (ID_SERIALIZATION_MAP.containsKey(idByte)) {
                logger.error(TRANSPORT_FAILED_SERIALIZATION, "", "", "Serialization extension " + serialization.getClass().getName()
                    + " has duplicate id to Serialization extension "
                    + ID_SERIALIZATION_MAP.get(idByte).getClass().getName()
                    + ", ignore this Serialization extension");
                continue;
            }
            ID_SERIALIZATION_MAP.put(idByte, serialization);
            ID_SERIALIZATIONNAME_MAP.put(idByte, name);
            SERIALIZATIONNAME_ID_MAP.put(name, idByte);
        }
    }

    private CodecSupport() {
    }

    public static Serialization getSerializationById(Byte id) {
        return ID_SERIALIZATION_MAP.get(id);
    }

    public static Byte getIDByName(String name) {
        return SERIALIZATIONNAME_ID_MAP.get(name);
    }

    public static Serialization getSerialization(URL url) {
        return url.getOrDefaultFrameworkModel().getExtensionLoader(Serialization.class).getExtension(UrlUtils.serializationOrDefault(url));
    }

    public static Serialization getSerialization(Byte id) throws IOException {
        Serialization result = getSerializationById(id);
        if (result == null) {
            throw new IOException("Unrecognized serialize type from consumer: " + id);
        }
        return result;
    }

    public static ObjectInput deserialize(URL url, InputStream is, byte proto) throws IOException {
        Serialization s = getSerialization(proto);
        return s.deserialize(url, is);
    }

    /**
     * Get the null object serialize result byte[] of Serialization from the cache,
     * if not, generate it first.
     *
     * @param s Serialization Instances
     * @return serialize result of null object
     */
    public static byte[] getNullBytesOf(Serialization s) {
        return ConcurrentHashMapUtils.computeIfAbsent(ID_NULLBYTES_MAP, s.getContentTypeId(), k -> {
            //Pre-generated Null object bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] nullBytes = new byte[0];
            try {
                ObjectOutput out = s.serialize(null, baos);
                out.writeObject(null);
                out.flushBuffer();
                nullBytes = baos.toByteArray();
                baos.close();
            } catch (Exception e) {
                logger.warn(TRANSPORT_FAILED_SERIALIZATION, "", "", "Serialization extension " + s.getClass().getName() + " not support serializing null object, return an empty bytes instead.");
            }
            return nullBytes;
        });
    }

    /**
     * Read all payload to byte[]
     *
     * @param is
     * @return
     * @throws IOException
     */
    public static byte[] getPayload(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = getBuffer(is.available());
        int len;
        while ((len = is.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        baos.flush();
        return baos.toByteArray();
    }

    private static byte[] getBuffer(int size) {
        byte[] bytes = TL_BUFFER.get();
        if (size <= bytes.length) {
            return bytes;
        }
        return new byte[size];
    }

    /**
     * Check if payload is null object serialize result byte[] of serialization
     *
     * @param payload
     * @param proto
     * @return
     */
    public static boolean isHeartBeat(byte[] payload, byte proto) {
        return Arrays.equals(payload, getNullBytesOf(getSerializationById(proto)));
    }

    public static void checkSerialization(String requestSerializeName, URL url) throws IOException {
        Collection<String> all = UrlUtils.allSerializations(url);
        checkSerialization(requestSerializeName, all);
    }

    public static void checkSerialization(String requestSerializeName, Collection<String> allSerializeName) throws IOException {
        for (String serialization : allSerializeName) {
            if (serialization.equals(requestSerializeName)) {
                return;
            }
        }
        throw new IOException("Unexpected serialization type:" + requestSerializeName + " received from network, please check if the peer send the right id.");
    }


}
