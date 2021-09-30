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

/**
 * The MetadataIdentifier is used to store method descriptor.
 * <p>
 * The name of class is reserved because of it has been used in the previous version.
 * <p>
 * 2018/10/25
 */
public class MetadataIdentifier extends BaseServiceMetadataIdentifier implements BaseMetadataIdentifier {

    private String application;

    public MetadataIdentifier() {
    }

    public MetadataIdentifier(String serviceInterface, String version, String group, String side, String application) {
        this.serviceInterface = serviceInterface;
        this.version = version;
        this.group = group;
        this.side = side;
        this.application = application;
    }


    public MetadataIdentifier(URL url) {
        this.serviceInterface = url.getServiceInterface();
        this.version = url.getVersion();
        this.group = url.getGroup();
        this.side = url.getSide();
        setApplication(url.getApplication());
    }

    public String getUniqueKey(KeyTypeEnum keyType) {
        return super.getUniqueKey(keyType, application);
    }

    public String getIdentifierKey() {
        return super.getIdentifierKey(application);
    }

    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

}
