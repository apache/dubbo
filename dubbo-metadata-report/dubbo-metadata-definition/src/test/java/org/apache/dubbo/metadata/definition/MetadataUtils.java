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
package org.apache.dubbo.metadata.definition;

import org.apache.dubbo.metadata.definition.model.MethodDefinition;
import org.apache.dubbo.metadata.definition.model.ServiceDefinition;
import org.apache.dubbo.metadata.definition.model.TypeDefinition;
import org.apache.dubbo.metadata.definition.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * generate metadata
 * <p>
 * 2017-4-17 14:33:24
 */
public class MetadataUtils {

    /**
     * com.taobao.hsf.metadata.store.MetadataInfoStoreServiceRedis.publishClassInfo(ServiceMetadata) 生成元数据的代码
     */
    public static ServiceDefinition generateMetadata(Class<?> interfaceClass) {
        ServiceDefinition sd = new ServiceDefinition();
        sd.setCanonicalName(interfaceClass.getCanonicalName());
        sd.setCodeSource(ClassUtils.getCodeSource(interfaceClass));

        TypeDefinitionBuilder builder = new TypeDefinitionBuilder();
        List<Method> methods = ClassUtils.getPublicNonStaticMethods(interfaceClass);
        for (Method method : methods) {
            MethodDefinition md = new MethodDefinition();
            md.setName(method.getName());

            Class<?>[] paramTypes = method.getParameterTypes();
            Type[] genericParamTypes = method.getGenericParameterTypes();

            String[] parameterTypes = new String[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                try {
                    TypeDefinition td = builder.build(genericParamTypes[i], paramTypes[i]);
                    parameterTypes[i] = td.getType();
                } catch (Exception e) {
                    parameterTypes[i] = paramTypes[i].getName();
                }
            }
            md.setParameterTypes(parameterTypes);
            try {
                TypeDefinition td = builder.build(method.getGenericReturnType(), method.getReturnType());
                md.setReturnType(td.getType());
            } catch (Exception e) {
                md.setReturnType(method.getReturnType().getName());
            }

            sd.getMethods().add(md);
        }

        sd.setTypes(builder.getTypeDefinitions());
        return sd;
    }
}
