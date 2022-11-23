/*
 * Copyright 2013-2015 the original author or authors.
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

package org.apache.dubbo.rpc.protocol.mvc.feign.annotation;

import feign.MethodMetadata;
import org.apache.dubbo.rpc.protocol.mvc.feign.AnnotatedParameterProcessor;
import org.springframework.web.bind.annotation.RequestHeader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * {@link RequestHeader} parameter processor.
 *
 * @author Jakub Narloch
 * @author Abhijit Sarkar
 * @see AnnotatedParameterProcessor
 */
public class RequestHeaderParameterProcessor implements AnnotatedParameterProcessor {

	private static final Class<RequestHeader> ANNOTATION = RequestHeader.class;

	@Override
	public Class<? extends Annotation> getAnnotationType() {
		return ANNOTATION;
	}

	@Override
	public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
		int parameterIndex = context.getParameterIndex();
		Class<?> parameterType = method.getParameterTypes()[parameterIndex];
		MethodMetadata data = context.getMethodMetadata();

		if (Map.class.isAssignableFrom(parameterType)) {
			checkState(data.headerMapIndex() == null, "Header map can only be present once.");
			data.headerMapIndex(parameterIndex);

			return true;
		}

		String name = ANNOTATION.cast(annotation).value();
		checkState(emptyToNull(name) != null,
				"RequestHeader.value() was empty on parameter %s", parameterIndex);
		context.setParameterName(name);

		Collection<String> header = context.setTemplateParameter(name, data.template().headers().get(name));
		data.template().header(name, header);
		return true;
	}
}
