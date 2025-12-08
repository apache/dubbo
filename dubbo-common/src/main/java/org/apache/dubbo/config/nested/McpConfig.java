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
package org.apache.dubbo.config.nested;

import org.apache.dubbo.config.support.Nested;

import java.io.Serializable;

public class McpConfig implements Serializable {

    private static final long serialVersionUID = 6943417856234001947L;

    /**
     * Whether to enable MCP Server support
     * <p>The default value is 'true'.
     */
    private Boolean enabled;

    /**
     * The port of MCP Server
     * <p>The default value is '0'.
     */
    private Integer port;

    /**
     * the path of mcp
     */
    @Nested
    private MCPPath path;

    /**
     * streamable or sse
     */
    private String protocol;

    /**
     * Session timeout in milliseconds for long connection
     * unit: seconds
     */
    private Integer sessionTimeout;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public MCPPath getPath() {
        return path;
    }

    public void setPath(MCPPath path) {
        this.path = path;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Integer sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public static class MCPPath implements Serializable {

        private static final long serialVersionUID = 6943417856234837947L;

        /**
         * The path of mcp message
         * <p>The default value is '/mcp/message'.
         */
        private String message;

        /**
         * The path of mcp sse
         * <p>The default value is '/mcp/sse'.
         */
        private String sse;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSse() {
            return sse;
        }

        public void setSse(String sse) {
            this.sse = sse;
        }
    }
}
