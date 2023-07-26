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
package org.apache.dubbo.aot.generate;

import java.util.Set;

/**
 * A describer that describes the need for reflection on a type.
 */
public class TypeDescriber implements ConditionalDescriber {

    private final String name;

    private final String reachableType;

    private final Set<FieldDescriber> fields;

    private final Set<ExecutableDescriber> constructors;

    private final Set<ExecutableDescriber> methods;

    private final Set<MemberCategory> memberCategories;

    public TypeDescriber(String name, String reachableType, Set<FieldDescriber> fields,
                         Set<ExecutableDescriber> constructors, Set<ExecutableDescriber> methods,
                         Set<MemberCategory> memberCategories) {
        this.name = name;
        this.reachableType = reachableType;
        this.fields = fields;
        this.constructors = constructors;
        this.methods = methods;
        this.memberCategories = memberCategories;
    }


    public String getName() {
        return name;
    }

    public Set<MemberCategory> getMemberCategories() {
        return memberCategories;
    }

    public Set<FieldDescriber> getFields() {
        return fields;
    }

    public Set<ExecutableDescriber> getConstructors() {
        return constructors;
    }

    public Set<ExecutableDescriber> getMethods() {
        return methods;
    }

    @Override
    public String getReachableType() {
        return reachableType;
    }
}
