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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.model.ServiceRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodecSupport {
    private static final Logger logger = LoggerFactory.getLogger(CodecSupport.class);
    private static Map<Byte, Serialization> ID_SERIALIZATION_MAP = new HashMap<Byte, Serialization>();
    private static Map<Byte, String> ID_SERIALIZATIONNAME_MAP = new HashMap<Byte, String>();
    private static Map<String, Byte> SERIALIZATIONNAME_ID_MAP = new HashMap<String, Byte>();
    // Cache null object serialize results, for heartbeat request/response serialize use.
    private static Map<Byte, byte[]> ID_NULLBYTES_MAP = new HashMap<Byte, byte[]>();

    private static final ThreadLocal<byte[]> TL_BUFFER = ThreadLocal.withInitial(() -> new byte[1024]);

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

    public static Byte getIDByName(String name) {
        return SERIALIZATIONNAME_ID_MAP.get(name);
    }

    public static Serialization getSerialization(URL url) {
        return ExtensionLoader.getExtensionLoader(Serialization.class).getExtension(
                url.getParameter(Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION));
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
     * Get the null object serialize result byte[] of Serialization from the cache,
     * if not, generate it first.
     *
     * @param s Serialization Instances
     * @return serialize result of null object
     */
    public static byte[] getNullBytesOf(Serialization s) {
        return ID_NULLBYTES_MAP.computeIfAbsent(s.getContentTypeId(), k -> {
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
                logger.warn("Serialization extension " + s.getClass().getName() + " not support serializing null object, return an empty bytes instead.");
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

    public static void checkSerialization(String path, String version, Byte id) throws IOException {
        ServiceRepository repository = ApplicationModel.getServiceRepository();
        ProviderModel providerModel = repository.lookupExportedServiceWithoutGroup(path + ":" + version);
        if (providerModel == null) {
            if (logger.isWarnEnabled()) {
                logger.warn("Serialization security check is enabled but cannot work as expected because " +
                        "there's no matched provider model for path " + path + ", version " + version);
            }
        } else {
            List<URL> urls = providerModel.getServiceConfig().getExportedUrls();
            if (CollectionUtils.isNotEmpty(urls)) {
                URL url = urls.get(0);
                String serializationName = url.getParameter(org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY, Constants.DEFAULT_REMOTING_SERIALIZATION);
                Byte localId = SERIALIZATIONNAME_ID_MAP.get(serializationName);
                if (localId != null && !localId.equals(id)) {
                    throw new IOException("Unexpected serialization id:" + id + " received from network, please check if the peer send the right id.");
                }
            }
        }
    }
}
