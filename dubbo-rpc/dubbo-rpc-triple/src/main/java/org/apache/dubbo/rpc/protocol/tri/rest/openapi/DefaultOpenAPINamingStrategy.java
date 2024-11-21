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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.MethodMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.openapi.model.OpenAPI;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

public class DefaultOpenAPINamingStrategy implements OpenAPINamingStrategy {

    @Override
    public String generateOperationId(MethodMeta methodMeta, OpenAPI openAPI) {
        return methodMeta.getMethod().getName();
    }

    @Override
    public String resolveOperationIdConflict(int attempt, String operationId, MethodMeta methodMeta, OpenAPI openAPI) {
        Method method = methodMeta.getMethod();
        if (attempt == 1) {
            String sig = TypeUtils.buildSig(method);
            if (sig != null) {
                return method.getName() + '_' + sig;
            }
        }
        return method.getName() + '_' + buildPostfix(attempt, method.toString());
    }

    @Override
    public String generateSchemaName(Class<?> clazz, OpenAPI openAPI) {
        return clazz.getSimpleName();
    }

    @Override
    public String resolveSchemaNameConflict(int attempt, String schemaName, Class<?> clazz, OpenAPI openAPI) {
        return clazz.getSimpleName() + '_' + buildPostfix(attempt, clazz.getName());
    }

    private static String buildPostfix(int attempt, String str) {
        if (attempt > 4) {
            str += ThreadLocalRandom.current().nextInt(10000);
        }
        return Bytes.bytes2hex(Bytes.getMD5(str), 0, Math.min(4, attempt));
    }
}
