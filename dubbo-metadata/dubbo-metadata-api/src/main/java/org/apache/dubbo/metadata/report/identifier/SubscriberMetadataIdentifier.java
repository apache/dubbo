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

import static org.apache.dubbo.common.constants.CommonConstants.REVISION_KEY;

/**
 * 2019-08-12
 */
public class SubscriberMetadataIdentifier extends BaseApplicationMetadataIdentifier implements BaseMetadataIdentifier {

    private String revision;

    public SubscriberMetadataIdentifier() {
    }

    public SubscriberMetadataIdentifier(String application, String revision) {
        this.application = application;
        this.revision = revision;
    }


    public SubscriberMetadataIdentifier(URL url) {
        this.application = url.getApplication("");
        this.revision = url.getParameter(REVISION_KEY, "");
    }

    public String getUniqueKey(KeyTypeEnum keyType) {
        return super.getUniqueKey(keyType, revision);
    }

    public String getIdentifierKey() {
        return super.getIdentifierKey(revision);
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }


}
