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
package org.apache.dubbo.common.extension;

/**
 * Uniform accessor for extension
 * ExtensionAccessor 扩展的统一访问器
 * • 用于获取扩展加载管理器 ExtensionDirector 对象
 * • 获取扩展对象 ExtensionLoader
 * • 根据扩展名字获取具体扩展对象
 * • 获取自适应扩展对象
 * • 获取默认扩展对象
 * • ScopeModel 模型对象的公共抽象父类型
 * • 内部 id 用于表示模型树的层次结构
 * • 公共模型名称，可以被用户设置
 * • 描述信息
 * • 类加载器管理
 * • 父模型管理 parent
 * • 当前模型的所属域 ExtensionScope 有:FRAMEWORK(框架)，APPLICATION (应用)，MODULE(模块)，SELF(自给自足，为每个作用域创建一个实例，
 * 用于特殊的 SPI 扩展，如 ExtensionInjector)
 * • 具体的扩展加载程序管理器对象的管理:ExtensionDirector
 * • 域 Bean 工厂管理，一个内部共享的 Bean 工厂 ScopeBeanFactory
 * • 等等
 */
public interface ExtensionAccessor {

    ExtensionDirector getExtensionDirector();

    default <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return this.getExtensionDirector().getExtensionLoader(type);
    }

    default <T> T getExtension(Class<T> type, String name) {
        ExtensionLoader<T> extensionLoader = getExtensionLoader(type);
        return extensionLoader != null ? extensionLoader.getExtension(name) : null;
    }

    default <T> T getAdaptiveExtension(Class<T> type) {
        ExtensionLoader<T> extensionLoader = getExtensionLoader(type);
        return extensionLoader != null ? extensionLoader.getAdaptiveExtension() : null;
    }

    default <T> T getDefaultExtension(Class<T> type) {
        ExtensionLoader<T> extensionLoader = getExtensionLoader(type);
        return extensionLoader != null ? extensionLoader.getDefaultExtension() : null;
    }

}
