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

import java.util.List;
import java.util.Optional;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * {@code MethodOrdererContext} encapsulates the <em>context</em> in which
 * a {@link MethodOrderer} will be invoked.
 *
 * @since 5.4
 * @see MethodOrderer
 * @see MethodDescriptor
 */
@API(status = EXPERIMENTAL, since = "5.4")
public interface MethodOrdererContext {

	/**
	 * Get the test class for this context.
	 *
	 * @return the test class; never {@code null}
	 */
	Class<?> getTestClass();

	/**
	 * Get the list of {@linkplain MethodDescriptor method descriptors} to
	 * order.
	 *
	 * @return the list of method descriptors; never {@code null}
	 */
	List<? extends MethodDescriptor> getMethodDescriptors();

	/**
	 * Get the configuration parameter stored under the specified {@code key}.
	 *
	 * <p>If no such key is present in the {@code ConfigurationParameters} for
	 * the JUnit Platform, an attempt will be made to look up the value as a
	 * JVM system property. If no such system property exists, an attempt will
	 * be made to look up the value in the JUnit Platform properties file.
	 *
	 * @param key the key to look up; never {@code null} or blank
	 * @return an {@code Optional} containing the value; never {@code null}
	 * but potentially empty
	 *
	 * @see System#getProperty(String)
	 * @see org.junit.platform.engine.ConfigurationParameters
	 */
	Optional<String> getConfigurationParameter(String key);

}
