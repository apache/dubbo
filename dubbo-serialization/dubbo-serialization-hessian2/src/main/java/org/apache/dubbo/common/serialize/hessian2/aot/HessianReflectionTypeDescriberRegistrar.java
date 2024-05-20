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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.com.caucho.hessian.io.BigDecimalDeserializer;
import com.alibaba.com.caucho.hessian.io.FileDeserializer;
import com.alibaba.com.caucho.hessian.io.HessianRemote;
import com.alibaba.com.caucho.hessian.io.LocaleSerializer;
import com.alibaba.com.caucho.hessian.io.ObjectNameDeserializer;
import com.alibaba.com.caucho.hessian.io.StringValueSerializer;
import com.alibaba.com.caucho.hessian.io.java8.DurationSerializer;
import com.alibaba.com.caucho.hessian.io.java8.InstantSerializer;
import com.alibaba.com.caucho.hessian.io.java8.LocalDateSerializer;
import com.alibaba.com.caucho.hessian.io.java8.LocalDateTimeSerializer;
import com.alibaba.com.caucho.hessian.io.java8.LocalTimeSerializer;
import com.alibaba.com.caucho.hessian.io.java8.MonthDaySerializer;
import com.alibaba.com.caucho.hessian.io.java8.OffsetDateTimeSerializer;
import com.alibaba.com.caucho.hessian.io.java8.OffsetTimeSerializer;
import com.alibaba.com.caucho.hessian.io.java8.PeriodSerializer;
import com.alibaba.com.caucho.hessian.io.java8.YearMonthSerializer;
import com.alibaba.com.caucho.hessian.io.java8.YearSerializer;
import com.alibaba.com.caucho.hessian.io.java8.ZoneIdSerializer;
import com.alibaba.com.caucho.hessian.io.java8.ZoneOffsetSerializer;
import com.alibaba.com.caucho.hessian.io.java8.ZonedDateTimeSerializer;

public class HessianReflectionTypeDescriberRegistrar implements ReflectionTypeDescriberRegistrar {

    @Override
    public List<TypeDescriber> getTypeDescribers() {
        List<TypeDescriber> typeDescribers = new ArrayList<>();
        typeDescribers.add(buildTypeDescriberWithDeclared(BigDecimalDeserializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(FileDeserializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(HessianRemote.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(LocaleSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(ObjectNameDeserializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(StringValueSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(DurationSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(InstantSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(LocalDateSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(LocalDateTimeSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(LocalTimeSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(MonthDaySerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(OffsetDateTimeSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(OffsetTimeSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(PeriodSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(YearMonthSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(YearSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(ZoneIdSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(ZoneOffsetSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(ZonedDateTimeSerializer.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(Object.class));
        typeDescribers.add(buildTypeDescriberWithDeclared(StackTraceElement.class));
        typeDescribers.add(buildTypeDescriberWithDeclared("sun.misc.Unsafe"));

        return typeDescribers;
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
        return new TypeDescriber(
                cl, null, new HashSet<>(), new HashSet<>(), new HashSet<>(), memberCategories);
    }
}
