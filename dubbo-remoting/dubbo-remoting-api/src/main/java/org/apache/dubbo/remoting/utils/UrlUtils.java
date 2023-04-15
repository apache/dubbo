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

package org.apache.dubbo.remoting.utils;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.support.DefaultSerializationSelector;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.transport.CodecSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.apache.dubbo.remoting.Constants.PREFER_SERIALIZATION_KEY;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;

public class UrlUtils {
    private static final String ALLOWED_SERIALIZATION_KEY = "allowedSerialization";

    public static int getIdleTimeout(URL url) {
        int heartBeat = getHeartbeat(url);
        // idleTimeout should be at least more than twice heartBeat because possible retries of client.
        int idleTimeout = url.getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartBeat * 3);
        if (idleTimeout < heartBeat * 2) {
            throw new IllegalStateException("idleTimeout < heartbeatInterval * 2");
        }
        return idleTimeout;
    }

    public static int getHeartbeat(URL url) {
        return url.getParameter(Constants.HEARTBEAT_KEY, Constants.DEFAULT_HEARTBEAT);
    }

    /**
     * Get the serialization id
     *
     * @param url url
     * @return {@link Byte}
     */
    public static Byte serializationId(URL url) {
        Byte serializationId;
        // Obtain the value from prefer_serialization. Such as.fastjson2,hessian2
        List<String> preferSerials = preferSerialization(url);
        for (String preferSerial : preferSerials) {
            if ((serializationId = CodecSupport.getIDByName(preferSerial)) != null) {
                return serializationId;
            }
        }

        // Secondly, obtain the value from serialization
        if ((serializationId = CodecSupport.getIDByName(url.getParameter(SERIALIZATION_KEY))) != null) {
            return serializationId;
        }

        // Finally, use the default serialization type
        return CodecSupport.getIDByName(DefaultSerializationSelector.getDefaultRemotingSerialization());
    }

    /**
     * Get the serialization or default serialization
     *
     * @param url url
     * @return {@link String}
     */
    public static String serializationOrDefault(URL url) {
        //noinspection OptionalGetWithoutIsPresent
        Optional<String> serializations = allSerializations(url).stream().findFirst();
        return serializations.orElseGet(DefaultSerializationSelector::getDefaultRemotingSerialization);
    }

    /**
     * Get the all serializations,ensure insertion order
     *
     * @param url url
     * @return {@link List}<{@link String}>
     */
    @SuppressWarnings("unchecked")
    public static Collection<String> allSerializations(URL url) {
        // preferSerialization -> serialization -> default serialization
        Set<String> serializations = new LinkedHashSet<>(preferSerialization(url));
        Optional.ofNullable(url.getParameter(SERIALIZATION_KEY)).filter(StringUtils::isNotBlank).ifPresent(serializations::add);
        serializations.add(DefaultSerializationSelector.getDefaultRemotingSerialization());
        return Collections.unmodifiableSet(serializations);
    }

    /**
     * Prefer Serialization
     *
     * @param url url
     * @return {@link List}<{@link String}>
     */
    public static List<String> preferSerialization(URL url) {
        String preferSerialization = url.getParameter(PREFER_SERIALIZATION_KEY);
        if (StringUtils.isNotBlank(preferSerialization)) {
            return Collections.unmodifiableList(StringUtils.splitToList(preferSerialization, ','));
        }
        return Collections.emptyList();
    }
}
