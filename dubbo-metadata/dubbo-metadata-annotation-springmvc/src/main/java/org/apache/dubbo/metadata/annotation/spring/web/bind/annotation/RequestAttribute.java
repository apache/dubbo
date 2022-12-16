/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.metadata.annotation.spring.web.bind.annotation;

import java.lang.annotation.*;

/**
 * Annotation to bind a method parameter to a request attribute.
 *
 * <p>The main motivation is to provide convenient access to request attributes
 * from a controller method with an optional/required check and a cast to the
 * target method parameter type.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 * @see RequestMapping
 * @see SessionAttribute
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestAttribute {

	/**
	 * Alias for {@link #name}.
	 */
    String value() default "";

	/**
	 * The name of the request attribute to bind to.
	 * <p>The default name is inferred from the method parameter name.
	 */
    String name() default "";

	/**
	 * Whether the request attribute is required.
	 * <p>Defaults to {@code true}, leading to an exception being thrown if
	 * the attribute is missing. Switch this to {@code false} if you prefer
	 * a {@code null} or Java 8 {@code java.util.Optional} if the attribute
	 * doesn't exist.
	 */
	boolean required() default true;

}
