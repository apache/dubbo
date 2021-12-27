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
package org.apache.dubbo.common.compiler;

import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.extension.SPI;

/**
 * Compiler. (SPI, Singleton, ThreadSafe)
 */
@SPI(value = "javassist", scope = ExtensionScope.FRAMEWORK)
public interface Compiler {

    /**
     * Compile java source code.
     *
     * @param code        Java source code
     * @param classLoader classloader
     * @return Compiled class
     * @deprecated use {@link Compiler#compile(Class, String, ClassLoader)} to support JDK 16
     */
    @Deprecated
    default Class<?> compile(String code, ClassLoader classLoader) {
        return compile(null, code, classLoader);
    }

    /**
     * Compile java source code.
     *
     * @param neighbor    A class belonging to the same package that this
     *                    class belongs to.  It is used to load the class. (For JDK 16 and above)
     * @param code        Java source code
     * @param classLoader classloader
     * @return Compiled class
     */
    default Class<?> compile(Class<?> neighbor, String code, ClassLoader classLoader) {
        return compile(code, classLoader);
    }

}
