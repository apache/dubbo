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
package com.alibaba.dubbo.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method grouping validation.
 * <p>
 * Scenario: this annotation can be used on interface's method when need to check against group before invoke the method
 * For example: <pre> @MethodValidated({Save.class, Update.class})
 * void relatedQuery(ValidationParameter parameter);
 * </pre>
 * It means both Save group and Update group are needed to check when method relatedQuery is invoked.
 * </p>
 */
/**
 * 方法分组验证注解
 * <p>使用场景：当调用某个方法时，需要检查多个分组，可以在接口方法上加上该注解</p><br>
 * 用法:<pre>   @MethodValidated({Save.class, Update.class})
 *  void relatedQuery(ValidationParameter parameter);</pre>
 *  在接口方法上增加注解,表示relatedQuery这个方法需要同时检查Save和Update这两个分组
 *
 * @author: zhangyinyue
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodValidated {

    /**
     * @return 分组集合
     */
    Class<?>[] value() default {};

}