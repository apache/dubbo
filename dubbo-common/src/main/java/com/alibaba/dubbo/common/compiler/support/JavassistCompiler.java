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

import com.alibaba.dubbo.common.utils.ClassHelper;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JavassistCompiler. (SPI, Singleton, ThreadSafe)
 */
public class JavassistCompiler extends AbstractCompiler {

    /**
     * 正则 - 匹配 import
     */
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([\\w\\.\\*]+);\n");
    /**
     * 正则 - 匹配 extends
     */
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\s+extends\\s+([\\w\\.]+)[^\\{]*\\{\n");
    /**
     * 正则 - 匹配 implements
     */
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("\\s+implements\\s+([\\w\\.]+)\\s*\\{\n");
    /**
     * 正则 - 匹配方法
     */
    private static final Pattern METHODS_PATTERN = Pattern.compile("\n(private|public|protected)\\s+");
    /**
     * 正则 - 匹配变量
     */
    private static final Pattern FIELD_PATTERN = Pattern.compile("[^\n]+=[^\n]+;");

    @Override
    public Class<?> doCompile(String name, String source) throws Throwable {
        // 获得类名
        int i = name.lastIndexOf('.');
        String className = i < 0 ? name : name.substring(i + 1);
        // 创建 ClassPool 对象
        ClassPool pool = new ClassPool(true);
        // 设置类搜索路径
        pool.appendClassPath(new LoaderClassPath(ClassHelper.getCallerClassLoader(getClass())));
        // 匹配 import
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        List<String> importPackages = new ArrayList<String>(); // 引用的包名
        Map<String, String> fullNames = new HashMap<String, String>(); // 引用的类名
        while (matcher.find()) {
            String pkg = matcher.group(1);
            if (pkg.endsWith(".*")) { // 引用整个包下的类/接口
                String pkgName = pkg.substring(0, pkg.length() - 2);
                pool.importPackage(pkgName);
                importPackages.add(pkgName);
            } else { // 引用指定类/接口
                int pi = pkg.lastIndexOf('.');
                if (pi > 0) {
                    String pkgName = pkg.substring(0, pi);
                    pool.importPackage(pkgName);
                    importPackages.add(pkgName);
                    fullNames.put(pkg.substring(pi + 1), pkg); // 类名
                }
            }
        }
        String[] packages = importPackages.toArray(new String[0]);
        // 匹配 extends
        matcher = EXTENDS_PATTERN.matcher(source);
        CtClass cls;
        if (matcher.find()) {
            String extend = matcher.group(1).trim();
            String extendClass;
            if (extend.contains(".")) { // 内嵌的类，例如：extends A.B
                extendClass = extend;
            } else if (fullNames.containsKey(extend)) { // 指定引用的类
                extendClass = fullNames.get(extend);
            } else { // 引用整个包下的类
                extendClass = ClassUtils.forName(packages, extend).getName();
            }
            // 创建 CtClass 对象
            cls = pool.makeClass(name, pool.get(extendClass));
        } else {
            // 创建 CtClass 对象
            cls = pool.makeClass(name);
        }
        // 匹配 implements
        matcher = IMPLEMENTS_PATTERN.matcher(source);
        if (matcher.find()) {
            String[] ifaces = matcher.group(1).trim().split("\\,");
            for (String iface : ifaces) {
                iface = iface.trim();
                String ifaceClass;
                if (iface.contains(".")) { // 内嵌的接口，例如：extends A.B
                    ifaceClass = iface;
                } else if (fullNames.containsKey(iface)) { // 指定引用的接口
                    ifaceClass = fullNames.get(iface);
                } else { // 引用整个包下的接口
                    ifaceClass = ClassUtils.forName(packages, iface).getName();
                }
                // 添加接口
                cls.addInterface(pool.get(ifaceClass));
            }
        }
        // 获得类中的内容，即首末 {} 的内容。
        String body = source.substring(source.indexOf("{") + 1, source.length() - 1);
        // 匹配 method 。使用分隔的方式，实际上，分隔出来的不仅仅有方法。
        String[] methods = METHODS_PATTERN.split(body);
        for (String method : methods) {
            method = method.trim();
            if (method.length() > 0) {
                if (method.startsWith(className)) { // 构造方法
                    cls.addConstructor(CtNewConstructor.make("public " + method, cls));
                } else if (FIELD_PATTERN.matcher(method).matches()) { // 变量
                    cls.addField(CtField.make("private " + method, cls));
                } else { // 方法
                    cls.addMethod(CtNewMethod.make("public " + method, cls));
                }
            }
        }
        // 生成类
        // JavassistCompiler.class.getProtectionDomain() =》 设置保护域和 JavassistCompiler 一致，即 `#getClass()` 方法。深入见 《Java安全——安全管理器、访问控制器和类装载器》https://www.zybuluo.com/changedi/note/417132
        return cls.toClass(ClassHelper.getCallerClassLoader(getClass()), JavassistCompiler.class.getProtectionDomain());
    }

}
