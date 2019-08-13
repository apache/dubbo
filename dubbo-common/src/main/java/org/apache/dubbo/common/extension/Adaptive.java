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

import org.apache.dubbo.common.URL;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide helpful information for {@link ExtensionLoader} to inject dependency extension instance.
 *
 * 1. 标记在类上，代表手动实现它是一个拓展接口的 Adaptive 拓展实现类。目前 Dubbo 项目里，只有 ExtensionFactory 拓展的实现类 AdaptiveExtensionFactory 有这么用
 * 2. 标记在拓展接口的方法上，代表自动生成代码实现该接口的 Adaptive 拓展实现类
 *     value ，从 Dubbo URL 获取参数中，使用键名( Key )，获取键值。该值为真正的拓展名
 *     自适应拓展实现类，会获取拓展名对应的真正的拓展对象。通过该对象，执行真正的逻辑
 *     可以设置多个键名( Key )，顺序获取直到有值。若最终获取不到，使用默认拓展名
 *
 * @see ExtensionLoader
 * @see URL
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Adaptive {
    /**
     * Decide which target extension to be injected. The name of the target extension is decided by the parameter passed
     * in the URL, and the parameter names are given by this method.
     * <p>
     * If the specified parameters are not found from {@link URL}, then the default extension will be used for
     * dependency injection (specified in its interface's {@link SPI}).
     * <p>
     * For examples, given <code>String[] {"key1", "key2"}</code>:
     * <ol>
     * <li>find parameter 'key1' in URL, use its value as the extension's name</li>
     * <li>try 'key2' for extension's name if 'key1' is not found (or its value is empty) in URL</li>
     * <li>use default extension if 'key2' doesn't appear either</li>
     * <li>otherwise, throw {@link IllegalStateException}</li>
     * </ol>
     * If default extension's name is not give on interface's {@link SPI}, then a name is generated from interface's
     * class name with the rule: divide classname from capital char into several parts, and separate the parts with
     * dot '.', for example: for {@code org.apache.dubbo.xxx.YyyInvokerWrapper}, its default name is
     * <code>String[] {"yyy.invoker.wrapper"}</code>. This name will be used to search for parameter from URL.
     *
     * @return parameter key names in URL
     *
     * 从 {@link URL }的 Key 名，对应的 Value 作为要 Adapt 成的 Extension 名。
     * <p>
     * 如果 {@link URL} 这些 Key 都没有 Value ，使用 缺省的扩展（在接口的{@link SPI}中设定的值）。<br>
     * 比如，<code>String[] {"key1", "key2"}</code>，表示
     * <ol>
     *      <li>先在URL上找key1的Value作为要Adapt成的Extension名；
     *      <li>key1没有Value，则使用key2的Value作为要Adapt成的Extension名。
     *      <li>key2没有Value，使用缺省的扩展。
     *      <li>如果没有设定缺省扩展，则方法调用会抛出{@link IllegalStateException}。
     * </ol>
     * <p>
     * 如果不设置则缺省使用Extension接口类名的点分隔小写字串。<br>
     * 即对于Extension接口 {@code com.alibaba.dubbo.xxx.YyyInvokerWrapper} 的缺省值为 <code>String[] {"yyy.invoker.wrapper"}</code>
     *
     * @see SPI#value()
     */
    String[] value() default {};

}