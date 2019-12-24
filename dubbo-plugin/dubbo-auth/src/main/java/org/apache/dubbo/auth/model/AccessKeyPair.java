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
package org.apache.dubbo.auth.model;

/**
 * The model of AK/SK pair
 */
public class AccessKeyPair {
    private String accessKey;
    private String secretKey;
    private String consumerSide;
    private String providerSide;
    private String creator;
    private String options;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getConsumerSide() {
        return consumerSide;
    }

    public void setConsumerSide(String consumerSide) {
        this.consumerSide = consumerSide;
    }

    public String getProviderSide() {
        return providerSide;
    }

    public void setProviderSide(String providerSide) {
        this.providerSide = providerSide;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "AccessKeyPair{" +
                "accessKey='" + accessKey + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", consumerSide='" + consumerSide + '\'' +
                ", providerSide='" + providerSide + '\'' +
                ", creator='" + creator + '\'' +
                ", options='" + options + '\'' +
                '}';
    }
}
