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
package org.apache.dubbo.common.serialize.hessian2.aot;

import org.apache.dubbo.aot.api.MemberCategory;
import org.apache.dubbo.aot.api.ReflectionTypeDescriberRegistrar;
import org.apache.dubbo.aot.api.TypeDescriber;

import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class HessianReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();

        loadFile("META-INF/dubbo/hessian/deserializers", typeDescribers);
        loadFile("META-INF/dubbo/hessian/serializers", typeDescribers);

        typeDescribers.add(buildTypeDescriberWithDeclared(Date.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(Time.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(Timestamp.class));

        return typeDescribers;
    }

    private void loadFile(String path, List<TypeDescriber> typeDescribers) {
        try {
            Enumeration<URL> resources = this.getClass().getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                Properties props = new Properties();
                props.load(url.openStream());
                for (Object value : props.values()) {
                    String className = (String) value;
                    typeDescribers.add(buildTypeDescriberWithDeclared(className));
                }
            }
        } catch (Throwable t) {
            // ignore
        }
    }

    private TypeDescriber buildTypeDescriberWithDeclared(Class<?> cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_METHODS);
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        memberCategories.add(MemberCategory.DECLARED_FIELDS);
        return new TypeDescriber(
                cl.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithDeclared(String cl) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_METHODS);
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        memberCategories.add(MemberCategory.DECLARED_FIELDS);
        return new TypeDescriber(cl, null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }
}
