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
package org.apache.dubbo.remoting.http.config;

public class HttpClientConfig {
    private int readTimeout = 6 * 1000;
    private int writeTimeout = 6 * 1000;
    private int connectTimeout = 6 * 1000;
    private int chunkLength = 8196;

    private int HTTP_CLIENT_CONNECTION_MANAGER_MAX_PER_ROUTE = 20;
    private int HTTP_CLIENT_CONNECTION_MANAGER_MAX_TOTAL = 20;
    private int HTTPCLIENT_KEEP_ALIVE_DURATION = 30 * 1000;
    private int HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_WAIT_TIME_MS = 1000;
    private int HTTP_CLIENT_CONNECTION_MANAGER_CLOSE_IDLE_TIME_S = 30;


    public HttpClientConfig() {
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getChunkLength() {
        return chunkLength;
    }
}
