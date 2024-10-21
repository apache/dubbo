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
package org.apache.dubbo.remoting.http12;

import java.nio.charset.StandardCharsets;

public final class HttpConstants {

    public static final String TRAILERS = "trailers";

    public static final String CHUNKED = "chunked";

    public static final String NO_CACHE = "no-cache";

    public static final String X_FORWARDED_PROTO = "x-forwarded-proto";
    public static final String X_FORWARDED_HOST = "x-forwarded-host";
    public static final String X_FORWARDED_PORT = "x-forwarded-port";

    public static final String HTTPS = "https";
    public static final String HTTP = "http";

    public static final byte[] SERVER_SENT_EVENT_DATA_PREFIX_BYTES = "data:".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] SERVER_SENT_EVENT_LF_BYTES = "\n\n".getBytes(StandardCharsets.US_ASCII);

    private HttpConstants() {}
}
