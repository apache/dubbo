/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.apache.dubbo.rpc.cluster.support.api;

import org.apiguardian.api.API;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * {@code @TestMethodOrder} is a type-level annotation that is used to configure
 * a {@link #value MethodOrderer} for the <em>test methods</em> of the annotated
 * test class or test interface.
 *
 * <p>In this context, the term "test method" refers to any method annotated with
 * {@code @Test}, {@code @RepeatedTest}, {@code @ParameterizedTest},
 * {@code @TestFactory}, or {@code @TestTemplate}.
 *
 * <p>If {@code @TestMethodOrder} is not explicitly declared on a test class,
 * inherited from a parent class, or declared on a test interface implemented by
 * a test class, test methods will be ordered using a default algorithm that is
 * deterministic but intentionally nonobvious.
 *
 * <h4>Example Usage</h4>
 *
 * <p>The following demonstrates how to guarantee that test methods are executed
 * in the order specified via the {@link Order @Order} annotation.
 *
 * <pre class="code">
 * {@literal @}TestMethodOrder(MethodOrderer.OrderAnnotation.class)
 * class OrderedTests {
 *
 *     {@literal @}Test
 *     {@literal @}Order(1)
 *     void nullValues() {}
 *
 *     {@literal @}Test
 *     {@literal @}Order(2)
 *     void emptyValues() {}
 *
 *     {@literal @}Test
 *     {@literal @}Order(3)
 *     void validValues() {}
 * }
 * </pre>
 *
 * @since 5.4
 * @see MethodOrderer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@API(status = EXPERIMENTAL, since = "5.4")
public @interface TestMethodOrder {

	/**
	 * The {@link MethodOrderer} to use.
	 *
	 * @see MethodOrderer
	 * @see MethodOrderer.Alphanumeric
	 * @see MethodOrderer.OrderAnnotation
	 * @see MethodOrderer.Random
	 */
	Class<? extends MethodOrderer> value();

}
