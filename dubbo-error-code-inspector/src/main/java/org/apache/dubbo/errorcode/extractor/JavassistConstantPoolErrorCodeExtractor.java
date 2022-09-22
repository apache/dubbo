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

package org.apache.dubbo.errorcode.extractor;

import org.apache.dubbo.errorcode.model.MethodDefinition;
import org.apache.dubbo.errorcode.util.ReflectUtils;

import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Error code extractor based on constant pool extracting with Javassist.
 */
public class JavassistConstantPoolErrorCodeExtractor implements ErrorCodeExtractor {

    @Override
    public List<String> getErrorCodes(String classFilePath) {

        ClassFile clsF = JavassistUtils.openClassFile(classFilePath);
        ConstPool cp = clsF.getConstPool();

        List<String> cpItems = JavassistUtils.getConstPoolStringItems(cp);

        return cpItems.stream().filter(x -> ERROR_CODE_PATTERN.matcher(x).matches()).collect(Collectors.toList());
    }

    @Override
    public List<MethodDefinition> getIllegalLoggerMethodInvocations(String classFilePath) {

        ClassFile classFile = JavassistUtils.openClassFile(classFilePath);
        List<Object> constPoolItems = JavassistUtils.getConstPoolItems(classFile.getConstPool());

        List<Integer> interfaceMethodRefIndices = constPoolItems.stream().filter(x -> {
            try {
                if (x == null) return false;
                return x.getClass() == Class.forName("javassist.bytecode.InterfaceMethodrefInfo");
            } catch (ClassNotFoundException e) {
                return false;
            }
        }).map(this::getIndexFieldInConstPoolItems).collect(Collectors.toList());

        List<MethodDefinition> methodDefinitions = new ArrayList<>();

        for (int index : interfaceMethodRefIndices) {
            ConstPool cp = classFile.getConstPool();

            MethodDefinition methodDefinition = new MethodDefinition();
            methodDefinition.setClassName(
                cp.getInterfaceMethodrefClassName(index)
            );

            methodDefinition.setMethodName(
                cp.getUtf8Info(
                    cp.getNameAndTypeName(
                        cp.getInterfaceMethodrefNameAndType(index)
                    )
                )
            );

            methodDefinition.setArguments(
                cp.getUtf8Info(
                    cp.getNameAndTypeDescriptor(
                        cp.getInterfaceMethodrefNameAndType(index)
                    )
                )
            );

            methodDefinitions.add(methodDefinition);
        }

        Predicate<MethodDefinition> legacyLoggerClass = x -> x.getClassName().equals("org.apache.dubbo.common.logger.Logger");
        Predicate<MethodDefinition> errorTypeAwareLoggerClass = x -> x.getClassName().equals("org.apache.dubbo.common.logger.ErrorTypeAwareLogger");
        Predicate<MethodDefinition> loggerClass = legacyLoggerClass.or(errorTypeAwareLoggerClass);

        return methodDefinitions.stream()
            .filter(loggerClass)
            .filter(x -> x.getMethodName().equals("warn") || x.getMethodName().equals("error"))
            .filter(x -> x.getArguments().split(";").length < 4)
            .collect(Collectors.toList());
    }

    private int getIndexFieldInConstPoolItems(Object item) {
        Field indexField = ReflectUtils.getDeclaredFieldRecursively(item.getClass(), "index");

        try {
            return (int) indexField.get(item);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
