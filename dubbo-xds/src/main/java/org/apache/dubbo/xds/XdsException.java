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
package org.apache.dubbo.xds;

public class XdsException extends RuntimeException {

    private Type type;

    public XdsException(Type type, String message) {
        super("[" + type + "]" + message);
        this.type = type;
    }

    public XdsException(Type type, String message, Throwable cause) {
        super("[" + type + "]" + message, cause);
        this.type = type;
    }

    public XdsException(Type type, Throwable cause) {
        super("[" + type + "]", cause);
        this.type = type;
    }

    public enum Type {
        EDS,
        CDS,
        SDS,
        LDS,
        RDS,
        ADS,
        SECURITY,
        UNKNOWN
    }
}
