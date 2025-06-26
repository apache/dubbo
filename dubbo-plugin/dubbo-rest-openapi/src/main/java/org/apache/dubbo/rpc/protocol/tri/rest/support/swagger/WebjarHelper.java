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
package org.apache.dubbo.rpc.protocol.tri.rest.support.swagger;

import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.remoting.http12.HttpResult;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.exception.HttpStatusException;

import java.io.IOException;
import java.io.InputStream;

import org.webjars.WebJarVersionLocator;

public class WebjarHelper {

    public static final boolean ENABLED = ClassUtils.isPresent("org.webjars.WebJarVersionLocator");
    private static volatile WebjarHelper INSTANCE;
    private final WebJarVersionLocator locator = new WebJarVersionLocator();

    public static WebjarHelper getInstance() {
        if (INSTANCE == null) {
            synchronized (WebjarHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WebjarHelper();
                }
            }
        }
        return INSTANCE;
    }

    public HttpResult<?> handleAssets(String webjar, String path) {
        try {
            byte[] bytes = getWebjarResource(webjar, path);
            if (bytes != null) {
                return HttpResult.builder()
                        .header("Cache-Control", "public, max-age=604800")
                        .body(bytes)
                        .build();
            }
        } catch (IOException ignored) {
        }
        throw new HttpStatusException(HttpStatus.NOT_FOUND.getCode());
    }

    public boolean hasWebjar(String webjar) {
        return locator.version(webjar) != null;
    }

    private byte[] getWebjarResource(String webjar, String exactPath) throws IOException {
        String fullPath = locator.fullPath(webjar, exactPath);
        if (fullPath != null) {
            InputStream is = WebJarVersionLocator.class.getClassLoader().getResourceAsStream(fullPath);
            if (is != null) {
                return StreamUtils.readBytes(is);
            }
        }
        return null;
    }
}
