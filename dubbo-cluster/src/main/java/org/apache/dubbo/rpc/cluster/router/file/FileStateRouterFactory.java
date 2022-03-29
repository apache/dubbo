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
package org.apache.dubbo.rpc.cluster.router.file;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.utils.IOUtils;
import org.apache.dubbo.rpc.cluster.router.script.ScriptStateRouterFactory;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;

import java.io.FileReader;
import java.io.IOException;

import static org.apache.dubbo.rpc.cluster.Constants.ROUTER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RULE_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.RUNTIME_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.TYPE_KEY;

public class FileStateRouterFactory implements StateRouterFactory {

    public static final String NAME = "file";

    private StateRouterFactory routerFactory;

    public void setRouterFactory(StateRouterFactory routerFactory) {
        this.routerFactory = routerFactory;
    }

    @Override
    public <T> StateRouter<T> getRouter(Class<T> interfaceClass, URL url) {
        try {
            // Transform File URL into Script Route URL, and Load
            // file:///d:/path/to/route.js?router=script ==> script:///d:/path/to/route.js?type=js&rule=<file-content>
            String protocol = url.getParameter(ROUTER_KEY, ScriptStateRouterFactory.NAME); // Replace original protocol (maybe 'file') with 'script'
            String type = null; // Use file suffix to config script type, e.g., js, groovy ...
            String path = url.getPath();
            if (path != null) {
                int i = path.lastIndexOf('.');
                if (i > 0) {
                    type = path.substring(i + 1);
                }
            }
            String rule = IOUtils.read(new FileReader(url.getAbsolutePath()));

            // FIXME: this code looks useless
            boolean runtime = url.getParameter(RUNTIME_KEY, false);
            URL script = URLBuilder.from(url)
                    .setProtocol(protocol)
                    .addParameter(TYPE_KEY, type)
                    .addParameter(RUNTIME_KEY, runtime)
                    .addParameterAndEncoded(RULE_KEY, rule)
                    .build();

            return routerFactory.getRouter(interfaceClass, script);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

}
