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
package org.apache.dubbo.qos.command.impl;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.qos.api.BaseCommand;
import org.apache.dubbo.qos.api.Cmd;
import org.apache.dubbo.qos.api.CommandContext;
import org.apache.dubbo.qos.api.PermissionLevel;
import org.apache.dubbo.remoting.http12.rest.OpenAPIRequest;
import org.apache.dubbo.remoting.http12.rest.OpenAPIService;
import org.apache.dubbo.rpc.model.FrameworkModel;

@Cmd(
        name = "getOpenAPI",
        summary = "Get the openapi descriptor for specified services.",
        example = {
            "getOpenAPI",
            "getOpenAPI groupA",
            "getOpenAPI com.example.DemoService",
            "getOpenAPI --group groupA --version 1.1.0 --tag tagA --service com.example. --openapi 3.0.0 --format yaml",
        },
        requiredPermissionLevel = PermissionLevel.PRIVATE)
public class GetOpenAPI implements BaseCommand {

    private final FrameworkModel frameworkModel;

    public GetOpenAPI(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public String execute(CommandContext commandContext, String[] args) {
        OpenAPIService openAPIService = frameworkModel.getBean(OpenAPIService.class);
        if (openAPIService == null) {
            return "OpenAPI is not available";
        }

        OpenAPIRequest request = new OpenAPIRequest();

        int len = args.length;
        if (len > 0) {
            if (len == 1) {
                String arg0 = args[0];
                if (arg0.indexOf('.') > 0) {
                    request.setService(new String[] {arg0});
                } else {
                    request.setGroup(arg0);
                }
            } else {
                for (int i = 0; i < len; i += 2) {
                    String value = args[i + 1];
                    switch (StringUtils.substringAfterLast(args[i], '-')) {
                        case "group":
                            request.setGroup(value);
                            break;
                        case "version":
                            request.setVersion(value);
                            break;
                        case "tag":
                            request.setTag(StringUtils.tokenize(value));
                            break;
                        case "service":
                            request.setService(StringUtils.tokenize(value));
                            break;
                        case "openapi":
                            request.setOpenapi(value);
                            break;
                        case "format":
                            request.setFormat(value);
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        return openAPIService.getDocument(request);
    }
}
