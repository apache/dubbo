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

import javassist.*;

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

    //正则-匹配import
    private static final Pattern IMPORT_PATTERN = Pattern.compile("import\\s+([\\w\\.\\*]+);\n");

    //正则-匹配extends
    private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\s+extends\\s+([\\w\\.]+)[^\\{]*\\{\n");

    //正则-匹配implements
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("\\s+implements\\s+([\\w\\.]+)\\s*\\{\n");

    //正则-匹配方法
    private static final Pattern METHODS_PATTERN = Pattern.compile("\n(private|public|protected)\\s+");

    //正则匹配变量
    private static final Pattern FIELD_PATTERN = Pattern.compile("[^\n]+=[^\n]+;");

    private String getShortClassName(String fullClassName){
        int i = fullClassName.lastIndexOf('.');
        return i < 0 ? fullClassName : fullClassName.substring(i + 1);
    }

    private void matchImport(String source,
                             ClassPool targetPool,
                             List<String> importPackages,
                             Map<String, String> fullNames) {
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            String pkg = matcher.group(1);
            //引用整个包下的类和接口
            if (pkg.endsWith(".*")) {
                String pkgName = pkg.substring(0, pkg.length() - 2);
                //引用包
                targetPool.importPackage(pkgName);
                importPackages.add(pkgName);
            } else {
                //引用指定类/接口
                int pi = pkg.lastIndexOf('.');
                if (pi > 0) {
                    String pkgName = pkg.substring(0, pi);
                    targetPool.importPackage(pkgName);
                    importPackages.add(pkgName);
                    fullNames.put(pkg.substring(pi + 1), pkg);
                }
            }
        }
    }

    private CtClass matchExtends(String fullClassName,
                              String source,
                              Map<String, String> fullNames,
                              ClassPool pool,
                              String[] packages) throws NotFoundException {
        Matcher matcher = EXTENDS_PATTERN.matcher(source);
        CtClass cls;
        if (matcher.find()) {
            String extend = matcher.group(1).trim();
            String extendClass;
            if (extend.contains(".")) {
                //内嵌的类，例如 extends A.B
                extendClass = extend;
            } else if (fullNames.containsKey(extend)) {
                //指定引用的类
                extendClass = fullNames.get(extend);
            } else {
                //引用的是包下的类
                extendClass = ClassUtils.forName(packages, extend).getName();
            }
            //创建带继承的类
            cls = pool.makeClass(fullClassName, pool.get(extendClass));
        } else {
            cls = pool.makeClass(fullClassName);
        }
        return cls;
    }

    private void matchImplements(String source,
                                 Map<String, String> fullNames,
                                 String[] packages,
                                 CtClass cls,
                                 ClassPool pool
    ) throws NotFoundException {
        Matcher matcher = IMPLEMENTS_PATTERN.matcher(source);
        if (matcher.find()) {
            String[] ifaces = matcher.group(1).trim().split("\\,");
            for (String iface : ifaces) {
                iface = iface.trim();
                String ifaceClass;
                if (iface.contains(".")) {
                    //内嵌的接口，例如 implements A.B
                    ifaceClass = iface;
                } else if (fullNames.containsKey(iface)) {
                    //指定引用的接口
                    ifaceClass = fullNames.get(iface);
                } else {
                    //引用整个包下的接口
                    ifaceClass = ClassUtils.forName(packages, iface).getName();
                }
                //添加实现的接口
                cls.addInterface(pool.get(ifaceClass));
            }
        }
    }

    private void matchMethod(String source,String fullClassName, CtClass cls) throws CannotCompileException {
        //获得类中的内容，即{}中的内容
        String body = source.substring(source.indexOf("{") + 1, source.length() - 1);
        //匹配方法(普通方法，构造方法，变量)
        String[] methods = METHODS_PATTERN.split(body);
        for (String method : methods) {
            method = method.trim();
            if (method.length() > 0) {
                if (method.startsWith(getShortClassName(fullClassName))) {
                    //添加构造方法
                    cls.addConstructor(CtNewConstructor.make("public " + method, cls));
                } else if (FIELD_PATTERN.matcher(method).matches()) {
                    //添加变量
                    cls.addField(CtField.make("private " + method, cls));
                } else {
                    //添加普通方法
                    cls.addMethod(CtNewMethod.make("public " + method, cls));
                }
            }
        }
    }

    @Override
    public Class<?> doCompile(String fullClassName, String source) throws Throwable {
        ClassPool pool = new ClassPool(true);
        //设置类搜索路径
        pool.appendClassPath(new LoaderClassPath(ClassHelper.getCallerClassLoader(getClass())));

        //匹配import
        List<String> importPackages = new ArrayList<String>();//报名
        //Map<类名,类的全路径>
        Map<String, String> fullNames = new HashMap<String, String>();
        matchImport(source, pool, importPackages, fullNames);
        String[] packages = importPackages.toArray(new String[0]);

        //匹配extends
        CtClass cls = matchExtends(fullClassName, source, fullNames, pool, packages);

        //匹配implements
        matchImplements(source, fullNames, packages, cls, pool);

        //匹配方法，变量，构造方法
        matchMethod(source,fullClassName, cls);

        //生成类
        return cls.toClass(ClassHelper.getCallerClassLoader(getClass()), JavassistCompiler.class.getProtectionDomain());
    }

}
