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

package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.serialize.ObjectInput;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.utils.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.dubbo.common.Constants.SERIALIZATION_KEY;

public class CodecSupport {

    private static final Logger logger = LoggerFactory.getLogger(CodecSupport.class);
    private static Map<Byte, Serialization> ID_SERIALIZATION_MAP = new HashMap<Byte, Serialization>();
    private static Map<Byte, String> ID_SERIALIZATIONNAME_MAP = new HashMap<Byte, String>();
    private static Map<String, Byte> SERIALIZATIONNAME_ID_MAP = new HashMap<String, Byte>();

    private static Map<String, Set<Byte>> PROVIDER_SUPPORTED_SERIALIZATION = new ConcurrentHashMap<String, Set<Byte>>();

    static {
        Set<String> supportedExtensions = ExtensionLoader.getExtensionLoader(Serialization.class).getSupportedExtensions();
        for (String name : supportedExtensions) {
            Serialization serialization = ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(name);
            byte idByte = serialization.getContentTypeId();
            if (ID_SERIALIZATION_MAP.containsKey(idByte)) {
                logger.error("Serialization extension " + serialization.getClass().getName()
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

    public static Serialization getSerialization(URL url) {
        return ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                url.getParameter(SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
    }

    public static Serialization getSerialization(URL url, Byte id) throws IOException {
        Serialization result = getSerializationById(id);
        if (result == null) {
            throw new IOException("Unrecognized serialize type from consumer: " + id);
        }
        return result;
    }

    public static ObjectInput deserialize(URL url, InputStream is, byte proto) throws IOException {
        Serialization s = getSerialization(url, proto);
        return s.deserialize(url, is);
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
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > -1) {
            baos.write(buffer, 0, len);
        }
        baos.flush();
        return baos.toByteArray();
    }

    public static Byte getIDByName(String name) {
        return SERIALIZATIONNAME_ID_MAP.get(name);
    }

    public static void checkSerialization(String path, String version, Byte id) throws IOException {
        Set<Byte> supportedSerialization = PROVIDER_SUPPORTED_SERIALIZATION.get(path + ":" + version);
        if (Constants.DEFAULT_VERSION.equals(version) && CollectionUtils.isEmpty(supportedSerialization)) {
            supportedSerialization = PROVIDER_SUPPORTED_SERIALIZATION.get(path);
        }
        if (CollectionUtils.isEmpty(supportedSerialization)) {
            if (logger.isWarnEnabled()) {
                logger.warn("Serialization security check is enabled but cannot work as expected because " +
                        "there's no matched provider model for path " + path + ", version " + version);
            }
        } else {
            if (!supportedSerialization.contains(id)) {
                throw new IOException("Unexpected serialization id:" + id + " received from network, please check if the peer send the right id.");
            }
        }
    }

    public static void addProviderSupportedSerialization(String serviceName, List<URL> exportedUrls) {
        if (CollectionUtils.isNotEmpty(exportedUrls)) {
            Set<Byte> supportedSerialization = new HashSet<Byte>();
            for (URL url : exportedUrls) {
                String serializationName = url.getParameter(SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION);
                Byte localId = SERIALIZATIONNAME_ID_MAP.get(serializationName);
                supportedSerialization.add(localId);
            }
            PROVIDER_SUPPORTED_SERIALIZATION.put(serviceName, Collections.unmodifiableSet(supportedSerialization));
        }
    }


}
