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
package org.apache.dubbo.monitor.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.monitor.MonitorService;

import java.io.Serializable;

/**
 * Statistics. (SPI, Prototype, ThreadSafe)
 */
public class Statistics implements Serializable {

    private static final long serialVersionUID = -6921183057683641441L;

    private URL url;

    private String application;

    private String service;

    private String method;

    private String group;

    private String version;

    private String client;

    private String server;

    public Statistics(URL url) {
        this.url = url;
        this.application = url.getParameter(MonitorService.APPLICATION);
        this.service = url.getParameter(MonitorService.INTERFACE);
        this.method = url.getParameter(MonitorService.METHOD);
        this.group = url.getParameter(MonitorService.GROUP);
        this.version = url.getParameter(MonitorService.VERSION);
        this.client = url.getParameter(MonitorService.CONSUMER, url.getAddress());
        this.server = url.getParameter(MonitorService.PROVIDER, url.getAddress());
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public String getApplication() {
        return application;
    }

    public Statistics setApplication(String application) {
        this.application = application;
        return this;
    }

    public String getService() {
        return service;
    }

    public Statistics setService(String service) {
        this.service = service;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMethod() {
        return method;
    }

    public Statistics setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getClient() {
        return client;
    }

    public Statistics setClient(String client) {
        this.client = client;
        return this;
    }

    public String getServer() {
        return server;
    }

    public Statistics setServer(String server) {
        this.server = server;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((application == null) ? 0 : application.hashCode());
        result = prime * result + ((client == null) ? 0 : client.hashCode());
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());
        result = prime * result + ((server == null) ? 0 : server.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Statistics other = (Statistics) obj;
        if (application == null) {
            if (other.application != null)
                return false;
        } else if (!application.equals(other.application))
            return false;
        if (client == null) {
            if (other.client != null)
                return false;
        } else if (!client.equals(other.client))
            return false;
        if (group == null) {
            if (other.group != null)
                return false;
        } else if (!group.equals(other.group))
            return false;
        if (method == null) {
            if (other.method != null)
                return false;
        } else if (!method.equals(other.method))
            return false;
        if (server == null) {
            if (other.server != null)
                return false;
        } else if (!server.equals(other.server))
            return false;
        if (service == null) {
            if (other.service != null)
                return false;
        } else if (!service.equals(other.service))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return url.toString();
    }

}