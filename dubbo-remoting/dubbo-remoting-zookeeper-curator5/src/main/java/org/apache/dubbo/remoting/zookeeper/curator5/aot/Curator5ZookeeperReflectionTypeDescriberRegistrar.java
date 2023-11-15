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
package org.apache.dubbo.remoting.zookeeper.curator5.aot;

import org.apache.dubbo.aot.api.MemberCategory;
import org.apache.dubbo.aot.api.ReflectionTypeDescriberRegistrar;
import org.apache.dubbo.aot.api.TypeDescriber;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.zookeeper.ClientCnxnSocketNIO;

public class Curator5ZookeeperReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        typeDescribers.add(buildTypeDescriberWithDeclaredConstructors(ClientCnxnSocketNIO.class));
        typeDescribers.add(buildTypeDescriberWithDeclared("org.apache.curator.x.discovery.ServiceInstance"));
        typeDescribers.add(buildTypeDescriberWithDeclared("org.apache.curator.x.discovery.details.OldServiceInstance"));
        return typeDescribers;
    }

    private TypeDescriber buildTypeDescriberWithDeclared(String className) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_METHODS);
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        memberCategories.add(MemberCategory.DECLARED_FIELDS);
        return new TypeDescriber(className, null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }

    private TypeDescriber buildTypeDescriberWithDeclaredConstructors(Class<?> c) {
        Set<MemberCategory> memberCategories = new HashSet<>();
        memberCategories.add(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
        return new TypeDescriber(
                c.getName(), null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }
}
