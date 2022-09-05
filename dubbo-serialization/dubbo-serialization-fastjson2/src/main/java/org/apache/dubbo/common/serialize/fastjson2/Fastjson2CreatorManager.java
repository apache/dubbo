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
package org.apache.dubbo.common.serialize.fastjson2;

import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ScopeClassLoaderListener;

import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.reader.ObjectReaderCreatorASM;
import com.alibaba.fastjson2.writer.ObjectWriterCreatorASM;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Fastjson2CreatorManager implements ScopeClassLoaderListener<FrameworkModel> {

    /**
     * An empty classLoader used when classLoader is system classLoader. Prevent the NPE.
     */
    private static final ClassLoader SYSTEM_CLASSLOADER_KEY = new ClassLoader() {};

    private final Map<ClassLoader, ObjectReaderCreatorASM> readerMap = new ConcurrentHashMap<>();
    private final Map<ClassLoader, ObjectWriterCreatorASM> writerMap = new ConcurrentHashMap<>();

    public Fastjson2CreatorManager(FrameworkModel frameworkModel) {
        frameworkModel.addClassLoaderListener(this);
    }

    public void setCreator(ClassLoader classLoader) {
        if (classLoader == null) {
            classLoader = SYSTEM_CLASSLOADER_KEY;
        }
        JSONFactory.setContextReaderCreator(readerMap.computeIfAbsent(classLoader, ObjectReaderCreatorASM::new));
        JSONFactory.setContextWriterCreator(writerMap.computeIfAbsent(classLoader, ObjectWriterCreatorASM::new));
    }

    @Override
    public void onAddClassLoader(FrameworkModel scopeModel, ClassLoader classLoader) {
        // nop
    }

    @Override
    public void onRemoveClassLoader(FrameworkModel scopeModel, ClassLoader classLoader) {
        readerMap.remove(classLoader);
        writerMap.remove(classLoader);
    }
}
