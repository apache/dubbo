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
package org.apache.dubbo.config.support;

import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.AbstractReferenceConfig;
import org.apache.dubbo.config.AbstractServiceConfig;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Parameter
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Parameter {
    /**
     * 键（别名）
     */
    String key() default "";
    /**
     * 是否必填
     */
    boolean required() default false;
    /**
     * 是否忽略
     */
    boolean excluded() default false;
    /**
     * 是否转义
     */
    boolean escaped() default false;
    /**
     * 是否为属性
     *
     * 目前用于《事件通知》https://dubbo.gitbooks.io/dubbo-user-book/demos/events-notify.html
     */
    boolean attribute() default false;
    /**
     * 是否拼接默认属性，参见 {@link AbstractConfig#appendParameters(Map, Object, String)} 方法。
     *
     * 我们来看看 `#append() = true` 的属性，有如下四个：
     * + {@link AbstractInterfaceConfig#getFilter()}
     * + {@link AbstractInterfaceConfig#getListener()}
     * + {@link AbstractReferenceConfig#getFilter()}
     * + {@link AbstractReferenceConfig#getListener()}
     * + {@link AbstractServiceConfig#getFilter()}
     * + {@link AbstractServiceConfig#getListener()}
     *
     * 那么，以 AbstractServiceConfig 举例子。
     *
     * 我们知道 ProviderConfig 和 ServiceConfig 继承 AbstractServiceConfig 类，那么 `filter` , `listener` 对应的相同的键。
     * 下面我们以 `filter` 举例子。
     *
     * 在 ServiceConfig 中，默认会<b>继承</b> ProviderConfig 配置的 `filter` 和 `listener` 。
     * 所以这个属性，就是用于，像 ServiceConfig 的这种情况，从 ProviderConfig 读取父属性。
     *
     * 举个例子，如果 `ProviderConfig.filter=aaaFilter` ，`ServiceConfig.filter=bbbFilter` ，最终暴露到 Dubbo URL 时，
     * 参数为 `service.filter=aaaFilter,bbbFilter` 。
     */
    boolean append() default false;

}