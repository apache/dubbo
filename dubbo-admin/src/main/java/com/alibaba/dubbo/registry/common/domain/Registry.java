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
package com.alibaba.dubbo.registry.common.domain;

import java.util.Date;

/**
 * Registry
 *
 */
public class Registry extends Entity {

    private static final long serialVersionUID = -8866645978415551309L;

    private String registry;/* registry address */

    private String url;

    private int connections = 0;/* connections number to registry */

    private Date expired;   /*time to expire*/

    private long alived;

    public Registry() {
    }

    public Registry(Long id) {
        super(id);
    }

    public Registry(String registryAddress, String consoleUrl, int aliveSeconds) {
        this.registry = registryAddress;
        this.url = consoleUrl;
        this.alived = aliveSeconds;
    }

    public String getAddress() {
        return registry;
    }

    public void setAddress(String registry) {
        this.registry = registry;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getExpired() {
        return expired;
    }

    public void setExpired(Date expiredDate) {
        this.expired = expiredDate;
    }

    public long getAlived() {
        return alived;
    }

    public void setAlived(long aliveSeconds) {
        this.alived = aliveSeconds;
    }


    public int getConnections() {
        return connections;
    }


    public void setConnections(int connections) {
        this.connections = connections;
    }

}
