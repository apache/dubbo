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
package org.apache.dubbo.metadata.annotation.processing.util;

import org.apache.dubbo.metadata.annotation.processing.AbstractAnnotationProcessingTest;
import org.apache.dubbo.metadata.annotation.processing.model.Model;
import org.apache.dubbo.metadata.tools.TestServiceImpl;

import org.junit.jupiter.api.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Set;

import static javax.lang.model.util.ElementFilter.fieldsIn;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.findMethod;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAllDeclaredFields;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAllDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getAllDeclaredMethods;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getDeclaredFields;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getDeclaredMembers;
import static org.apache.dubbo.metadata.annotation.processing.util.ModelUtils.getDeclaredMethods;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ModelUtils} Test
 *
 * @since 2.7.5
 */
public class ModelUtilsTest extends AbstractAnnotationProcessingTest {

    private TypeElement testType;

    @Override
    protected void addCompiledClasses(Set<Class<?>> classesToBeCompiled) {
    }

    @Override
    protected void beforeEach() {
        testType = getType(TestServiceImpl.class);
    }

    @Test
    public void testDeclaredMembers() {
        TypeElement type = getType(Model.class);
        List<? extends Element> members = getDeclaredMembers(type.asType());
        List<VariableElement> fields = fieldsIn(members);
        assertEquals(19, members.size());
        assertEquals(6, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());

        members = getAllDeclaredMembers(type.asType());
        fields = fieldsIn(members);
        assertEquals(11, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());
        assertEquals("b", fields.get(6).getSimpleName().toString());
        assertEquals("s", fields.get(7).getSimpleName().toString());
        assertEquals("i", fields.get(8).getSimpleName().toString());
        assertEquals("l", fields.get(9).getSimpleName().toString());
        assertEquals("z", fields.get(10).getSimpleName().toString());
    }

    @Test
    public void testDeclaredFields() {
        TypeElement type = getType(Model.class);
        List<VariableElement> fields = getDeclaredFields(type.asType());
        assertEquals(6, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());

        fields = getAllDeclaredFields(type.asType());
        assertEquals(11, fields.size());
        assertEquals("f", fields.get(0).getSimpleName().toString());
        assertEquals("d", fields.get(1).getSimpleName().toString());
        assertEquals("tu", fields.get(2).getSimpleName().toString());
        assertEquals("str", fields.get(3).getSimpleName().toString());
        assertEquals("bi", fields.get(4).getSimpleName().toString());
        assertEquals("bd", fields.get(5).getSimpleName().toString());
        assertEquals("b", fields.get(6).getSimpleName().toString());
        assertEquals("s", fields.get(7).getSimpleName().toString());
        assertEquals("i", fields.get(8).getSimpleName().toString());
        assertEquals("l", fields.get(9).getSimpleName().toString());
        assertEquals("z", fields.get(10).getSimpleName().toString());
    }

    @Test
    public void testDeclaredMethods() {
        TypeElement type = getType(Model.class);
        List<ExecutableElement> methods = getDeclaredMethods(type.asType());
        assertEquals(12, methods.size());

        methods = getAllDeclaredMethods(type.asType());
        assertEquals(34, methods.size());
    }

    @Test
    public void testFindMethod() {
        TypeElement type = getType(Model.class);
        // Test methods from java.lang.Object
        // Object#toString()
        String methodName = "toString";
        ExecutableElement method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#hashCode()
        methodName = "hashCode";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#getClass()
        methodName = "getClass";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#finalize()
        methodName = "finalize";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#clone()
        methodName = "clone";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#notify()
        methodName = "notify";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#notifyAll()
        methodName = "notifyAll";
        method = findMethod(type.asType(), methodName);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#wait(long)
        methodName = "wait";
        method = findMethod(type.asType(), methodName, long.class);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#wait(long,int)
        methodName = "wait";
        method = findMethod(type.asType(), methodName, long.class, int.class);
        assertEquals(method.getSimpleName().toString(), methodName);

        // Object#equals(Object)
        methodName = "equals";
        method = findMethod(type.asType(), methodName, Object.class);
        assertEquals(method.getSimpleName().toString(), methodName);
    }

    @Test
    public void testMatchParameterTypes() {

    }
}
