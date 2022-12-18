/*
 * Copyright 2002-2021 the original author or authors.
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
 * Annotation to indicate that a method parameter is bound to an HTTP cookie.
 *
 * <p>The method parameter may be declared as type {@link javax.servlet.http.Cookie}
 * or as cookie value type (String, int, etc.).
 *
 * <p>Note that with spring-webmvc 5.3.x and earlier, the cookie value is URL
 * decoded. This will be changed in 6.0 but in the meantime, applications can
 * also declare parameters of type {@link javax.servlet.http.Cookie} to access
 * the raw value.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see RequestMapping
 * @see RequestParam
 * @see RequestHeader
 * @see RequestMapping
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CookieValue {

	/**
	 * Alias for {@link #name}.
	 */
    String value() default "";

	/**
	 * The name of the cookie to bind to.
	 * @since 4.2
	 */
    String name() default "";

	/**
	 * Whether the cookie is required.
	 * <p>Defaults to {@code true}, leading to an exception being thrown
	 * if the cookie is missing in the request. Switch this to
	 * {@code false} if you prefer a {@code null} value if the cookie is
	 * not present in the request.
	 * <p>Alternatively, provide a {@link #defaultValue}, which implicitly
	 * sets this flag to {@code false}.
	 */
	boolean required() default true;

	/**
	 * The default value to use as a fallback.
	 * <p>Supplying a default value implicitly sets {@link #required} to
	 * {@code false}.
	 */
	String defaultValue() default ValueConstants.DEFAULT_NONE;

}
