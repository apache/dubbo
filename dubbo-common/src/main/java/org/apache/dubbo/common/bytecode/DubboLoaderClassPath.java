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
package org.apache.dubbo.common.bytecode;

import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.io.InputStream;
import java.net.URL;

/**
 * Ensure javassist will load Dubbo's class from Dubbo's classLoader
 */
public class DubboLoaderClassPath extends LoaderClassPath {
    public DubboLoaderClassPath() {
        super(DubboLoaderClassPath.class.getClassLoader());
    }

    @Override
    public InputStream openClassfile(String classname) throws NotFoundException {
        if (!classname.startsWith("org.apache.dubbo") && !classname.startsWith("grpc.health") && !classname.startsWith("com.google")) {
            return null;
        }
        return super.openClassfile(classname);
    }

    @Override
    public URL find(String classname) {
        if (!classname.startsWith("org.apache.dubbo")) {
            return null;
        }
        return super.find(classname);
    }
}
