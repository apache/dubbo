/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.rpc.protocol.mvc.feign;

import feign.MethodMetadata;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Feign contract method parameter processor.
 *
 * @author Jakub Narloch
 * @author Abhijit Sarkar
 */

// TODO add more annotation support,support resteasy annotation
public interface AnnotatedParameterProcessor {

	/**
	 * Retrieves the processor supported annotation type.
	 *
	 * @return the annotation type
	 */
	Class<? extends Annotation> getAnnotationType();

	/**
	 * Process the annotated parameter.
	 *
	 * @param context	the parameter context
	 * @param annotation the annotation instance
	 * @param method	 the method that contains the annotation
	 * @return whether the parameter is http
	 */
	boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method);

	/**
	 * Specifies the parameter context.
	 *
	 * @author Jakub Narloch
	 */
	interface AnnotatedParameterContext {

		/**
		 * Retrieves the method metadata.
		 *
		 * @return the method metadata
		 */
		MethodMetadata getMethodMetadata();

		/**
		 * Retrieves the index of the parameter.
		 *
		 * @return the parameter index
		 */
		int getParameterIndex();

		/**
		 * Sets the parameter name.
		 *
		 * @param name the name of the parameter
		 */
		void setParameterName(String name);

		/**
		 * Sets the template parameter.
		 *
		 * @param name the template parameter
		 * @param rest the existing parameter values
		 * @return parameters
		 */
		Collection<String> setTemplateParameter(String name, Collection<String> rest);
	}
}
