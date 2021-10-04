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
package org.apache.dubbo.metadata.report.identifier;

import org.apache.dubbo.common.URL;

import static org.apache.dubbo.metadata.MetadataConstants.KEY_REVISON_PREFIX;

/**
 * The ServiceMetadataIdentifier is used to store the {@link org.apache.dubbo.common.URL}
 * that are from provider and consumer
 * <p>
 * 2019-08-09
 */
public class ServiceMetadataIdentifier extends BaseServiceMetadataIdentifier implements BaseMetadataIdentifier {

    private String revision;
    private String protocol;

    public ServiceMetadataIdentifier() {
    }

    public ServiceMetadataIdentifier(String serviceInterface, String version, String group, String side, String revision, String protocol) {
        this.serviceInterface = serviceInterface;
        this.version = version;
        this.group = group;
        this.side = side;
        this.revision = revision;
        this.protocol = protocol;
    }


    public ServiceMetadataIdentifier(URL url) {
        this.serviceInterface = url.getServiceInterface();
        this.version = url.getVersion();
        this.group = url.getGroup();
        this.side = url.getSide();
        this.protocol = url.getProtocol();
    }

    public String getUniqueKey(KeyTypeEnum keyType) {
        return super.getUniqueKey(keyType, protocol, KEY_REVISON_PREFIX + revision);
    }

    public String getIdentifierKey() {
        return super.getIdentifierKey(protocol, KEY_REVISON_PREFIX + revision);
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String toString() {
        return "ServiceMetadataIdentifier{" +
                "revision='" + revision + '\'' +
                ", protocol='" + protocol + '\'' +
                ", serviceInterface='" + serviceInterface + '\'' +
                ", version='" + version + '\'' +
                ", group='" + group + '\'' +
                ", side='" + side + '\'' +
                "} " + super.toString();
    }
}
