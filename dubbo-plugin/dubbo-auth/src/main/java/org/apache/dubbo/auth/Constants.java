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
package org.apache.dubbo.auth;


public interface Constants {
    String REFERENCE_AUTH = "reference.auth";

    String SERVICE_AUTH = "service.auth";

    String AUTH_HELPER = "auth.helper";

    String DEFAULT_AUTH_HELPER = "accesskey";

    String DEFAULT_ACCESS_KEY_STORAGE = "urlstorage";

    String ACCESS_KEY_STORAGE_KEY = "accessKey.storage";

    String ACCESS_KEY_ID_KEY = "accessKeyId";

    String SECRET_ACCESS_KEY_KEY = "secretAccessKey";

    String REQUEST_TIMESTAMP_KEY = "timestamp";

    String REQUEST_SIGNATURE_KEY = "signature";

    String AK_KEY = "ak";

    String SIGNATURE_STRING_FORMAT = "%s#%s#%s#%s";

    String PARAMTER_ENCRYPT_ENABLE_KEY = "paramater.sign";
}
