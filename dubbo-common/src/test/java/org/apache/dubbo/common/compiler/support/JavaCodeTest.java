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
package org.apache.dubbo.common.compiler.support;

import java.util.concurrent.atomic.AtomicInteger;

public class JavaCodeTest {

    public final static AtomicInteger SUBFIX = new AtomicInteger(8);

    String getSimpleCode() {

        return "package org.apache.dubbo.common.compiler.support;" +
                "public class HelloServiceImpl" + SUBFIX.getAndIncrement() + " implements HelloService {" +
                "   public String sayHello() { " +
                "       return \"Hello world!\"; " +
                "   }" +
                "}";
    }

    String getSimpleCodeWithoutPackage(){
        return "public class HelloServiceImpl" + SUBFIX.getAndIncrement() + "implements org.apache.dubbo" +
                ".common.compiler.support.HelloService.HelloService {" +
                "   public String sayHello() { " +
                "       return \"Hello world!\"; " +
                "   }" +
                "}";
    }

    String getSimpleCodeWithSyntax(){

        // code.append("   }");
        // }
        return "package org.apache.dubbo.common.compiler.support;" +
                "public class HelloServiceImpl" + SUBFIX.getAndIncrement() + " implements HelloService {" +
                "   public String sayHello() { " +
                "       return \"Hello world!\"; ";
    }

    // only used for javassist
    String getSimpleCodeWithSyntax0(){

        // code.append("   }");
        // }
        return "package org.apache.dubbo.common.compiler.support;" +
                "public class HelloServiceImpl_0 implements HelloService {" +
                "   public String sayHello() { " +
                "       return \"Hello world!\"; ";
    }

    String getSimpleCodeWithImports() {

        return "package org.apache.dubbo.common.compiler.support;" +
                "import java.lang.*;\n" +
                "import org.apache.dubbo.common.compiler.support;\n" +
                "public class HelloServiceImpl2" + SUBFIX.getAndIncrement() + " implements HelloService {" +
                "   public String sayHello() { " +
                "       return \"Hello world!\"; " +
                "   }" +
                "}";
    }

    String getSimpleCodeWithWithExtends() {

        return "package org.apache.dubbo.common.compiler.support;" +
                "import java.lang.*;\n" +
                "import org.apache.dubbo.common.compiler.support;\n" +
                "public class HelloServiceImpl" + SUBFIX.getAndIncrement() + " extends org.apache.dubbo.common" +
                ".compiler.support.HelloServiceImpl0 {\n" +
                "   public String sayHello() { " +
                "       return \"Hello world3!\"; " +
                "   }" +
                "}";
    }
}
