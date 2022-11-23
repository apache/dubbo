/*
 * Copyright 2013-2016 the original author or authors.
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

package org.apache.dubbo.rpc.protocol.mvc.feign.support;

import feign.*;
import org.apache.dubbo.rpc.protocol.mvc.feign.AnnotatedParameterProcessor;
import org.apache.dubbo.rpc.protocol.mvc.feign.annotation.PathVariableParameterProcessor;
import org.apache.dubbo.rpc.protocol.mvc.feign.annotation.RequestHeaderParameterProcessor;
import org.apache.dubbo.rpc.protocol.mvc.feign.annotation.RequestParamParameterProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;
import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

/**
 * @author Spencer Gibb
 * @author Abhijit Sarkar
 */
public class SpringMvcContract extends Contract.BaseContract
		implements ResourceLoaderAware {

	private static final String ACCEPT = "Accept";

	private static final String CONTENT_TYPE = "Content-Type";

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

	private final Map<Class<? extends Annotation>, AnnotatedParameterProcessor> annotatedArgumentProcessors;
	private final Map<String, Method> processedMethods = new HashMap<>();

	private final ConversionService conversionService;
	private final Param.Expander expander;
	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	public SpringMvcContract() {
		this(Collections.<AnnotatedParameterProcessor> emptyList());
	}

	public SpringMvcContract(
			List<AnnotatedParameterProcessor> annotatedParameterProcessors) {
		this(annotatedParameterProcessors, new DefaultConversionService());
	}

	public SpringMvcContract(
			List<AnnotatedParameterProcessor> annotatedParameterProcessors,
			ConversionService conversionService) {
		Assert.notNull(annotatedParameterProcessors,
				"Parameter processors can not be null.");
		Assert.notNull(conversionService, "ConversionService can not be null.");

		List<AnnotatedParameterProcessor> processors;
		if (!annotatedParameterProcessors.isEmpty()) {
			processors = new ArrayList<>(annotatedParameterProcessors);
		}
		else {
			processors = getDefaultAnnotatedArgumentsProcessors();
		}
		this.annotatedArgumentProcessors = toAnnotatedArgumentProcessorMap(processors);
		this.conversionService = conversionService;
		this.expander = new ConvertingExpander(conversionService);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	protected void processAnnotationOnClass(MethodMetadata data, Class<?> clz) {
		if (clz.getInterfaces().length == 0) {
			RequestMapping classAnnotation = findMergedAnnotation(clz,
					RequestMapping.class);
			if (classAnnotation != null) {
				// Prepend path from class annotation if specified
				if (classAnnotation.value().length > 0) {
					String pathValue = emptyToNull(classAnnotation.value()[0]);
					pathValue = resolve(pathValue);
					if (!pathValue.startsWith("/")) {
						pathValue = "/" + pathValue;
					}
					data.template().insert(0, pathValue);
				}
			}
		}
	}

	@Override
	public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
		this.processedMethods.put(Feign.configKey(targetType, method), method);
		MethodMetadata md = super.parseAndValidateMetadata(targetType, method);

		RequestMapping classAnnotation = findMergedAnnotation(targetType,
				RequestMapping.class);
		if (classAnnotation != null) {
			// produces - use from class annotation only if method has not specified this
			if (!md.template().headers().containsKey(ACCEPT)) {
				parseProduces(md, method, classAnnotation);
			}

			// consumes -- use from class annotation only if method has not specified this
			if (!md.template().headers().containsKey(CONTENT_TYPE)) {
				parseConsumes(md, method, classAnnotation);
			}

			// headers -- class annotation is inherited to methods, always write these if
			// present
			parseHeaders(md, method, classAnnotation);
		}
		return md;
	}

	@Override
	protected void processAnnotationOnMethod(MethodMetadata data,
			Annotation methodAnnotation, Method method) {
		if (!RequestMapping.class.isInstance(methodAnnotation) && !methodAnnotation
				.annotationType().isAnnotationPresent(RequestMapping.class)) {
			return;
		}

		RequestMapping methodMapping = findMergedAnnotation(method, RequestMapping.class);
		// HTTP Method
		RequestMethod[] methods = methodMapping.method();
		if (methods.length == 0) {
			methods = new RequestMethod[] { RequestMethod.GET };
		}
		checkOne(method, methods, "method");
		data.template().method(methods[0].name());

		// path
		checkAtMostOne(method, methodMapping.value(), "value");
		if (methodMapping.value().length > 0) {
			String pathValue = emptyToNull(methodMapping.value()[0]);
			if (pathValue != null) {
				pathValue = resolve(pathValue);
				// Append path from @RequestMapping if value is present on method
				if (!pathValue.startsWith("/")
						&& !data.template().toString().endsWith("/")) {
					pathValue = "/" + pathValue;
				}
				data.template().append(pathValue);
			}
		}

		// produces
		parseProduces(data, method, methodMapping);

		// consumes
		parseConsumes(data, method, methodMapping);

		// headers
		parseHeaders(data, method, methodMapping);

		data.indexToExpander(new LinkedHashMap<Integer, Param.Expander>());
	}

	private String resolve(String value) {
		if (StringUtils.hasText(value)
				&& this.resourceLoader instanceof ConfigurableApplicationContext) {
			return ((ConfigurableApplicationContext) this.resourceLoader).getEnvironment()
					.resolvePlaceholders(value);
		}
		return value;
	}

	private void checkAtMostOne(Method method, Object[] values, String fieldName) {
		checkState(values != null && (values.length == 0 || values.length == 1),
				"Method %s can only contain at most 1 %s field. Found: %s",
				method.getName(), fieldName,
				values == null ? null : Arrays.asList(values));
	}

	private void checkOne(Method method, Object[] values, String fieldName) {
		checkState(values != null && values.length == 1,
				"Method %s can only contain 1 %s field. Found: %s", method.getName(),
				fieldName, values == null ? null : Arrays.asList(values));
	}

	@Override
	protected boolean processAnnotationsOnParameter(MethodMetadata data,
			Annotation[] annotations, int paramIndex) {
		boolean isHttpAnnotation = false;

		AnnotatedParameterProcessor.AnnotatedParameterContext context = new SimpleAnnotatedParameterContext(
				data, paramIndex);
		Method method = this.processedMethods.get(data.configKey());
		for (Annotation parameterAnnotation : annotations) {
			AnnotatedParameterProcessor processor = this.annotatedArgumentProcessors
					.get(parameterAnnotation.annotationType());
			if (processor != null) {
				Annotation processParameterAnnotation;
				// synthesize, handling @AliasFor, while falling back to parameter name on
				// missing String #value():
				processParameterAnnotation = synthesizeWithMethodParameterNameAsFallbackValue(
						parameterAnnotation, method, paramIndex);
				isHttpAnnotation |= processor.processArgument(context,
						processParameterAnnotation, method);
			}
		}
		if (isHttpAnnotation && data.indexToExpander().get(paramIndex) == null
				&& this.conversionService.canConvert(
						method.getParameterTypes()[paramIndex], String.class)) {
			data.indexToExpander().put(paramIndex, this.expander);
		}
		return isHttpAnnotation;
	}

	private void parseProduces(MethodMetadata md, Method method,
			RequestMapping annotation) {
		checkAtMostOne(method, annotation.produces(), "produces");
		String[] serverProduces = annotation.produces();
		String clientAccepts = serverProduces.length == 0 ? null
				: emptyToNull(serverProduces[0]);
		if (clientAccepts != null) {
			md.template().header(ACCEPT, clientAccepts);
		}
	}

	private void parseConsumes(MethodMetadata md, Method method,
			RequestMapping annotation) {
		checkAtMostOne(method, annotation.consumes(), "consumes");
		String[] serverConsumes = annotation.consumes();
		String clientProduces = serverConsumes.length == 0 ? null
				: emptyToNull(serverConsumes[0]);
		if (clientProduces != null) {
			md.template().header(CONTENT_TYPE, clientProduces);
		}
	}

	private void parseHeaders(MethodMetadata md, Method method,
			RequestMapping annotation) {
		// TODO: only supports one header value per key
		if (annotation.headers() != null && annotation.headers().length > 0) {
			for (String header : annotation.headers()) {
				int index = header.indexOf('=');
				if (!header.contains("!=") && index >= 0) {
					md.template().header(resolve(header.substring(0, index)),
						resolve(header.substring(index + 1).trim()));
				}
			}
		}
	}

	private Map<Class<? extends Annotation>, AnnotatedParameterProcessor> toAnnotatedArgumentProcessorMap(
			List<AnnotatedParameterProcessor> processors) {
		Map<Class<? extends Annotation>, AnnotatedParameterProcessor> result = new HashMap<>();
		for (AnnotatedParameterProcessor processor : processors) {
			result.put(processor.getAnnotationType(), processor);
		}
		return result;
	}

	private List<AnnotatedParameterProcessor> getDefaultAnnotatedArgumentsProcessors() {

		List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

		annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
		annotatedArgumentResolvers.add(new RequestParamParameterProcessor());
		annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());

		return annotatedArgumentResolvers;
	}

	private Annotation synthesizeWithMethodParameterNameAsFallbackValue(
			Annotation parameterAnnotation, Method method, int parameterIndex) {
		Map<String, Object> annotationAttributes = AnnotationUtils
				.getAnnotationAttributes(parameterAnnotation);
		Object defaultValue = AnnotationUtils.getDefaultValue(parameterAnnotation);
		if (defaultValue instanceof String
				&& defaultValue.equals(annotationAttributes.get(AnnotationUtils.VALUE))) {
			Type[] parameterTypes = method.getGenericParameterTypes();
			String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(method);
			if (shouldAddParameterName(parameterIndex, parameterTypes, parameterNames)) {
				annotationAttributes.put(AnnotationUtils.VALUE,
						parameterNames[parameterIndex]);
			}
		}
		return AnnotationUtils.synthesizeAnnotation(annotationAttributes,
				parameterAnnotation.annotationType(), null);
	}

	private boolean shouldAddParameterName(int parameterIndex, Type[] parameterTypes, String[] parameterNames) {
		// has a parameter name
		return parameterNames != null && parameterNames.length > parameterIndex
				// has a type
				&& parameterTypes != null && parameterTypes.length > parameterIndex;
	}

	private class SimpleAnnotatedParameterContext
			implements AnnotatedParameterProcessor.AnnotatedParameterContext {

		private final MethodMetadata methodMetadata;

		private final int parameterIndex;

		public SimpleAnnotatedParameterContext(MethodMetadata methodMetadata,
				int parameterIndex) {
			this.methodMetadata = methodMetadata;
			this.parameterIndex = parameterIndex;
		}

		@Override
		public MethodMetadata getMethodMetadata() {
			return this.methodMetadata;
		}

		@Override
		public int getParameterIndex() {
			return this.parameterIndex;
		}

		@Override
		public void setParameterName(String name) {
			nameParam(this.methodMetadata, name, this.parameterIndex);
		}

		@Override
		public Collection<String> setTemplateParameter(String name,
				Collection<String> rest) {
			return addTemplatedParam(rest, name);
		}
	}

	public static class ConvertingExpander implements Param.Expander {

		private final ConversionService conversionService;

		public ConvertingExpander(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public String expand(Object value) {
			return this.conversionService.convert(value, String.class);
		}

	}
}
