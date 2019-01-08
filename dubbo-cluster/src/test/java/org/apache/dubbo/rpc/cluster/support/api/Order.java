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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * {@code @Order} is an annotation that is used to configure the
 * {@linkplain #value order} in which the annotated element (i.e., field or
 * method) should be evaluated or executed relative to other elements of the
 * same category.
 *
 * <p>When used with
 * {@link org.junit.jupiter.api.extension.RegisterExtension @RegisterExtension},
 * the category applies to <em>extension fields</em>. When used with the
 * {@link } {@link MethodOrderer}, the category applies to
 * <em>test methods</em>.
 *
 * <p>If {@code @Order} is not explicitly declared on an element, the default
 * order value assigned to the element is {@link Integer#MAX_VALUE}.
 *
 * @see MethodOrderer.OrderAnnotation
 * @see org.junit.jupiter.api.extension.RegisterExtension @RegisterExtension
 * @since 5.4
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@API(status = EXPERIMENTAL, since = "5.4")
public @interface Order {

    /**
     * The order value for the annotated element (i.e., field or method).
     *
     * <p>Elements are ordered based on priority where a lower value has greater
     * priority than a higher value. For example, {@link Integer#MAX_VALUE} has
     * the lowest priority.
     */
    int value();

}
