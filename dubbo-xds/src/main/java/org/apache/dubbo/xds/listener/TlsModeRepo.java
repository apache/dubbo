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
package org.apache.dubbo.xds.listener;

import org.apache.dubbo.xds.listener.TlsModeListener.TlsType;

import java.util.Collections;
import java.util.Map;

public class TlsModeRepo {

    public TlsModeRepo() {}


    private Map<String, TlsType> connectionType = Collections.emptyMap();

    private TlsType globalConfig;

    public void update(Map<String, TlsType> connectionType) {
        this.connectionType = connectionType;
    }

    public void setGlobalConfig(TlsType tlsType) {
        this.globalConfig = tlsType;
    }

    public TlsType getType(String indicator) {
        if (globalConfig != null) {
            return globalConfig;
        } else {
            return connectionType.get(indicator);
        }
    }
}
