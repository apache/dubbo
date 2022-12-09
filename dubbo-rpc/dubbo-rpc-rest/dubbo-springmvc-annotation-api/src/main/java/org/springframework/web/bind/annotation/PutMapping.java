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

package org.springframework.web.bind.annotation;

import java.lang.annotation.*;

/**
 * Annotation for mapping HTTP {@code PUT} requests onto specific handler
 * methods.
 *
 * <p>Specifically, {@code @PutMapping} is a <em>composed annotation</em> that
 * acts as a shortcut for {@code @RequestMapping(method = RequestMethod.PUT)}.
 *
 * @author Sam Brannen
 * @since 4.3
 * @see GetMapping
 * @see PostMapping
 * @see DeleteMapping
 * @see PatchMapping
 * @see RequestMapping
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.PUT)
public @interface PutMapping {

	/**
	 * Alias for {@link RequestMapping#name}.
	 */
    String name() default "";

	/**
	 * Alias for {@link RequestMapping#value}.
	 */
    String[] value() default {};

	/**
	 * Alias for {@link RequestMapping#path}.
	 */
    String[] path() default {};

	/**
	 * Alias for {@link RequestMapping#params}.
	 */
    String[] params() default {};

	/**
	 * Alias for {@link RequestMapping#headers}.
	 */
    String[] headers() default {};

	/**
	 * Alias for {@link RequestMapping#consumes}.
	 */
    String[] consumes() default {};

	/**
	 * Alias for {@link RequestMapping#produces}.
	 */
    String[] produces() default {};

}
