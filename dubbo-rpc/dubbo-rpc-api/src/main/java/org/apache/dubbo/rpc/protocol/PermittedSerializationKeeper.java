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
package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.remoting.utils.UrlUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.BaseServiceMetadata.interfaceFromServiceKey;
import static org.apache.dubbo.common.BaseServiceMetadata.versionFromServiceKey;

public class PermittedSerializationKeeper {
    private final ConcurrentMap<String, Set<Byte>> serviceToSerializationId = new ConcurrentHashMap<>();
    private final Set<Byte> globalPermittedSerializationIds = new ConcurrentHashSet<>();

    public void registerService(URL url) {
        Set<Byte> set = ConcurrentHashMapUtils.computeIfAbsent(serviceToSerializationId, keyWithoutGroup(url.getServiceKey()), k -> new ConcurrentHashSet<>());
        Collection<String> serializations = UrlUtils.allSerializations(url);
        for (String serialization : serializations) {
            Byte id = CodecSupport.getIDByName(serialization);
            if (id != null) {
                set.add(id);
                globalPermittedSerializationIds.add(id);
            }
        }
    }

    public boolean checkSerializationPermitted(String serviceKeyWithoutGroup, Byte id) throws IOException {
        Set<Byte> set = serviceToSerializationId.get(serviceKeyWithoutGroup);
        if (set == null) {
            throw new IOException("Service " + serviceKeyWithoutGroup + " not found, invocation rejected.");
        }
        return set.contains(id);
    }

    private static String keyWithoutGroup(String serviceKey) {
        String interfaceName = interfaceFromServiceKey(serviceKey);
        String version = versionFromServiceKey(serviceKey);
        if (StringUtils.isEmpty(version)) {
            return interfaceName;
        }
        return interfaceName + CommonConstants.GROUP_CHAR_SEPARATOR + version;
    }
}
