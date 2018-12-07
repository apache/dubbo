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
package org.apache.dubbo.metadata.identifier;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;

/**
 * 2018/10/25
 */
public class ConsumerMetadataIdentifier extends MetadataIdentifier {

    public ConsumerMetadataIdentifier() {
    }

    public ConsumerMetadataIdentifier(String serviceInterface, String version, String group, String application) {
        super(serviceInterface, version, group, Constants.CONSUMER_SIDE);
        this.application = application;
    }

    public ConsumerMetadataIdentifier(URL url) {
        super(url);
        setSide(Constants.CONSUMER_SIDE);
        setApplication(url.getParameter(Constants.APPLICATION_KEY));
    }

    private String application;

    protected String getPathSegment() {
        return Constants.PATH_SEPARATOR + application;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }
}
