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

package com.alibaba.dubbo.common.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for extension interface
 * <p/>
 * Changes on extension configuration file <br/>
 * Use <code>Protocol</code> as an example, its configuration file 'META-INF/dubbo/com.xxx.Protocol' is changes from: <br/>
 * <pre>
 *     com.foo.XxxProtocol
 *     com.foo.YyyProtocol
 * </pre>
 * <p>
 * to key-value pair <br/>
 * <pre>
 *     xxx=com.foo.XxxProtocol
 *     yyy=com.foo.YyyProtocol
 * </pre>
 * <br/>
 * The reason for this change is:
 * <p>
 * If there's third party library referenced by static field or by method in extension implementation, its class will
 * fail to initialize if the third party library doesn't exist. In this case, dubbo cannot figure out extension's id
 * therefore cannot be able to map the exception information with the extension, if the previous format is used.
 * <p/>
 * For example:
 * <p>
 * Fails to load Extension("mina"). When user configure to use mina, dubbo will complain the extension cannot be loaded,
 * instead of reporting which extract extension implementation fails and the extract reason.
 * </p>
 */

/**
 * 扩展点接口的标识。
 * 扩展点声明配置文件，格式修改。
 * 以Protocol示例，配置文件META-INF/dubbo/com.xxx.Protocol内容：
 * 由
 *      com.foo.XxxProtocol
 *      com.foo.YyyProtocol
 * 改成使用KV格式
 *      xxx=com.foo.XxxProtocol
 *      yyy=com.foo.YyyProtocol
 *
 * 原因：
 * 当扩展点的static字段或方法签名上引用了三方库，
 * 如果三方库不存在，会导致类初始化失败，
 * Extension标识Dubbo就拿不到了，异常信息就和配置对应不起来。
 *
 * 比如:
 * Extension("mina")加载失败，
 * 当用户配置使用mina时，就会报找不到扩展点，
 * 而不是报加载扩展点失败，以及失败原因。
 *
 * @author william.liangf
 * @author ding.lid
 * @export
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    /**
     * default extension name
     *
     * 默认拓展名
     */
    String value() default "";

}