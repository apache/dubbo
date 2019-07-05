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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;

/**
 * TODO Comment of ProtocolUtils
 */
public class ProtocolUtils {

    private static Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private static ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    public static <T> T refer(Class<T> type, String url) {
        return refer(type, URL.valueOf(url));
    }

    public static <T> T refer(Class<T> type, URL url) {
        return proxy.getProxy(protocol.refer(type, url));
    }

    public static <T> Exporter<T> export(T instance, Class<T> type, String url) {
        return export(instance, type, URL.valueOf(url));
    }

    public static <T> Exporter<T> export(T instance, Class<T> type, URL url) {
        return protocol.export(proxy.getInvoker(instance, type, url));
    }

}