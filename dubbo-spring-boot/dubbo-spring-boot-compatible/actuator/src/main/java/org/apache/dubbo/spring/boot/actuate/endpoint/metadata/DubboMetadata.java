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
package org.apache.dubbo.spring.boot.actuate.endpoint.metadata;

import org.apache.dubbo.common.Version;
import org.apache.dubbo.spring.boot.util.DubboUtils;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_GITHUB_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_MAILING_LIST;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SPRING_BOOT_GITHUB_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SPRING_BOOT_GIT_URL;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SPRING_BOOT_ISSUES_URL;

/**
 * Dubbo Metadata
 * @since 2.7.0
 */
@Component
public class DubboMetadata {

    public Map<String, Object> invoke() {

        Map<String, Object> metaData = new LinkedHashMap<>();

        metaData.put("timestamp", System.currentTimeMillis());

        Map<String, String> versions = new LinkedHashMap<>();
        versions.put("dubbo-spring-boot", Version.getVersion(DubboUtils.class, "1.0.0"));
        versions.put("dubbo", Version.getVersion());

        Map<String, String> urls = new LinkedHashMap<>();
        urls.put("dubbo", DUBBO_GITHUB_URL);
        urls.put("mailing-list", DUBBO_MAILING_LIST);
        urls.put("github", DUBBO_SPRING_BOOT_GITHUB_URL);
        urls.put("issues", DUBBO_SPRING_BOOT_ISSUES_URL);
        urls.put("git", DUBBO_SPRING_BOOT_GIT_URL);

        metaData.put("versions", versions);
        metaData.put("urls", urls);

        return metaData;
    }
}
