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
package com.alibaba.dubbo.common.compiler.support;


import com.alibaba.dubbo.common.compiler.Compiler;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.ExtensionLoader;

/**
 * AdaptiveCompiler. (SPI, Singleton, ThreadSafe)
 *
 * 自适应 Compiler 实现类
 */
@Adaptive
public class AdaptiveCompiler implements Compiler {

    /**
     * 默认编辑器的拓展名
     */
    private static volatile String DEFAULT_COMPILER;

    public static void setDefaultCompiler(String compiler) {
        DEFAULT_COMPILER = compiler;
    }

    @Override public Class<?> compile(String code, ClassLoader classLoader) {
        Compiler compiler;
        // 获得 Compiler 的 ExtensionLoader 对象。
        ExtensionLoader<Compiler> loader = ExtensionLoader.getExtensionLoader(Compiler.class);
        String name = DEFAULT_COMPILER; // copy reference
        // 使用设置的拓展名，获得 Compiler 拓展对象
        if (name != null && name.length() > 0) {
            compiler = loader.getExtension(name);
        // 获得默认的 Compiler 拓展对象
        } else {
            compiler = loader.getDefaultExtension();
        }
        //
        return compiler.compile(code, classLoader);
    }

}