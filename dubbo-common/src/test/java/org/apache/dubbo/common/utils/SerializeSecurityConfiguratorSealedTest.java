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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.io.Serializable;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

/**
 * Auto-trust must reach the subtypes a sealed type permits, because declaring a sealed type in a
 * service signature means the subtypes are what actually travel on the wire.
 *
 * <p>The sealed fixture is generated at runtime rather than written as source: this module compiles
 * at Java 8, where {@code sealed} does not exist.
 */
class SerializeSecurityConfiguratorSealedTest {

    private static final int CLASS_FILE_JAVA_17 = 61;

    /** Ordinary interface and implementation — the control for the sealed case. */
    public interface OpenParent extends Serializable {}

    public static class OpenChild implements OpenParent {}

    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    void testSealedSubclassesAreAutoTrusted() throws Exception {
        String pkg = getClass().getPackage().getName();
        String parentName = pkg + ".GeneratedSealedParent";
        String firstChildName = pkg + ".GeneratedSealedChildOne";
        String secondChildName = pkg + ".GeneratedSealedChildTwo";

        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(getClass().getClassLoader()));

        CtClass parent = pool.makeInterface(parentName);
        parent.addInterface(pool.get(Serializable.class.getName()));
        permitSubclasses(parent, firstChildName, secondChildName);
        Class<?> parentClass = parent.toClass(getClass());

        for (String childName : new String[] {firstChildName, secondChildName}) {
            CtClass child = pool.makeClass(childName);
            child.addInterface(parent);
            child.setModifiers(Modifier.PUBLIC | Modifier.FINAL);
            child.getClassFile().setMajorVersion(CLASS_FILE_JAVA_17);
            child.toClass(getClass());
        }

        // Guard the fixture itself: if the JVM does not see the class as sealed, the assertions
        // below would pass or fail for reasons unrelated to the code under test.
        Assertions.assertTrue(
                (Boolean) Class.class.getMethod("isSealed").invoke(parentClass),
                "generated fixture is not sealed; the test would prove nothing");

        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            ApplicationModel applicationModel = frameworkModel.newApplication();
            ModuleModel moduleModel = applicationModel.newModule();
            SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

            SerializeSecurityConfigurator configurator = new SerializeSecurityConfigurator(moduleModel);
            configurator.registerInterface(parentClass);

            Assertions.assertTrue(ssm.getAllowedPrefix().contains(parentName));
            Assertions.assertTrue(ssm.getAllowedPrefix().contains(firstChildName));
            Assertions.assertTrue(ssm.getAllowedPrefix().contains(secondChildName));
        } finally {
            frameworkModel.destroy();
        }
    }

    /**
     * A non-sealed hierarchy is open, so its implementations cannot be enumerated and must not be
     * trusted. This is what keeps the change from widening trust to every subtype.
     */
    @Test
    void testOpenSubclassesAreNotAutoTrusted() {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            ApplicationModel applicationModel = frameworkModel.newApplication();
            ModuleModel moduleModel = applicationModel.newModule();
            SerializeSecurityManager ssm = frameworkModel.getBeanFactory().getBean(SerializeSecurityManager.class);

            SerializeSecurityConfigurator configurator = new SerializeSecurityConfigurator(moduleModel);
            configurator.registerInterface(OpenParent.class);

            Assertions.assertTrue(ssm.getAllowedPrefix().contains(OpenParent.class.getName()));
            Assertions.assertFalse(ssm.getAllowedPrefix().contains(OpenChild.class.getName()));
        } finally {
            frameworkModel.destroy();
        }
    }

    private static void permitSubclasses(CtClass sealedClass, String... permittedNames) {
        ClassFile classFile = sealedClass.getClassFile();
        // PermittedSubclasses is only honoured from class file version 61 (Java 17) onwards.
        classFile.setMajorVersion(CLASS_FILE_JAVA_17);

        ConstPool constPool = classFile.getConstPool();
        byte[] info = new byte[2 + permittedNames.length * 2];
        info[0] = (byte) (permittedNames.length >>> 8);
        info[1] = (byte) permittedNames.length;
        for (int i = 0; i < permittedNames.length; i++) {
            int classIndex = constPool.addClassInfo(permittedNames[i]);
            info[2 + i * 2] = (byte) (classIndex >>> 8);
            info[3 + i * 2] = (byte) classIndex;
        }
        classFile.addAttribute(new AttributeInfo(constPool, "PermittedSubclasses", info));
    }
}
