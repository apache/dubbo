package com.alibaba.dubbo.rpc.protocol.springmvc.support;

/**
 * Created by wuyu on 2016/6/8.
 */

import feign.Contract;
import feign.MethodMetadata;
import feign.Util;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static feign.Util.checkState;
import static feign.Util.emptyToNull;

/**
 * @author Spencer Gibb
 */
public class SpringMvcContract extends Contract.BaseContract {

    private static final String ACCEPT = "Accept";

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String CLIENT_TYPE = "Client-Type";


    @Override
    public List<MethodMetadata> parseAndValidatateMetadata(Class<?> targetType) {
        checkState(targetType.getTypeParameters().length == 0, "Parameterized types unsupported: %s",
                targetType.getSimpleName());

        Map<String, MethodMetadata> result = new LinkedHashMap<String, MethodMetadata>();
        for (Method method : targetType.getMethods()) {
            if (method.getDeclaringClass() == Object.class ||
                    (method.getModifiers() & Modifier.STATIC) != 0 ||
                    Util.isDefault(method) || method.getAnnotation(RequestMapping.class) == null) {
                continue;
            }
            MethodMetadata metadata = parseAndValidateMetadata(targetType, method);
            checkState(!result.containsKey(metadata.configKey()), "Overrides unsupported: %s",
                    metadata.configKey());
            result.put(metadata.configKey(), metadata);
        }
        return new ArrayList<MethodMetadata>(result.values());
    }

    @Override
    public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
        MethodMetadata md = super.parseAndValidateMetadata(targetType, method);

        RequestMapping classAnnotation = method.getDeclaringClass().getAnnotation(RequestMapping.class);

        if (classAnnotation == null) {
            classAnnotation = targetType.getAnnotation(RequestMapping.class);
        }

        if (classAnnotation != null) {
            // Prepend path from class annotation if specified
            if (classAnnotation.value().length > 0) {
                String pathValue = emptyToNull(classAnnotation.value()[0]);
                checkState(pathValue != null, "RequestMapping.value() was empty on type %s",
                        method.getDeclaringClass().getName());
                if (!pathValue.startsWith("/")) {
                    pathValue = "/" + pathValue;
                }
                md.template().insert(0, pathValue);
            }

            // produces - use from class annotation only if method has not specified this
            if (!md.template().headers().containsKey(ACCEPT)) {
                parseProduces(md, method, classAnnotation);
            }

            // consumes -- use from class annotation only if method has not specified this
            if (!md.template().headers().containsKey(CONTENT_TYPE)) {
                parseConsumes(md, method, classAnnotation);
            }

            parseParams(md, method, classAnnotation);

            // headers -- class annotation is inherited to methods, always write these if present
            parseHeaders(md, method, classAnnotation);


        }
        return md;
    }

    @Override
    protected void processAnnotationOnMethod(MethodMetadata data,
                                             Annotation methodAnnotation, Method method) {
        if (!(methodAnnotation instanceof RequestMapping)) {
            return;
        }

        RequestMapping methodMapping = RequestMapping.class.cast(methodAnnotation);
        // HTTP Method
        checkOne(method, methodMapping.method(), "method");
        data.template().method(methodMapping.method()[0].name());

        // path
        checkAtMostOne(method, methodMapping.value(), "value");
        if (methodMapping.value().length > 0) {
            String pathValue = emptyToNull(methodMapping.value()[0]);
            if (pathValue != null) {
                // Append path from @RequestMapping if value is present on method
                if (!pathValue.startsWith("/") && !data.template().toString().endsWith("/")) {
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

        //add Client-Type Header
        if(!data.template().headers().containsKey(CLIENT_TYPE)){
            data.template().header(CLIENT_TYPE, "Feign-Client");
        }

        parseParams(data, method, methodMapping);


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
        // TODO: support spring parameter annotations?
        for (Annotation parameterAnnotation : annotations) {
            Class<? extends Annotation> annotationType = parameterAnnotation
                    .annotationType();
            if (annotationType == PathVariable.class) {
                String name = PathVariable.class.cast(parameterAnnotation).value();
                checkState(emptyToNull(name) != null,
                        "PathVariable annotation was empty on param %s.", paramIndex);
                nameParam(data, name, paramIndex);
                isHttpAnnotation = true;
                String varName = '{' + name + '}';
                if (data.template().url().indexOf(varName) == -1
                        && !searchMapValues(data.template().queries(), varName)
                        && !searchMapValues(data.template().headers(), varName)) {
                    data.formParams().add(name);
                }
            } else if (annotationType == RequestParam.class) {
                String name = RequestParam.class.cast(parameterAnnotation).value();
                checkState(emptyToNull(name) != null,
                        "QueryParam.value() was empty on parameter %s", paramIndex);
                Collection<String> query = addTemplatedParam(data.template().queries()
                        .get(name), name);
                data.template().query(name, query);
                nameParam(data, name, paramIndex);
                isHttpAnnotation = true;
            } else if (annotationType == RequestHeader.class) {
                String name = RequestHeader.class.cast(parameterAnnotation).value();
                checkState(emptyToNull(name) != null,
                        "HeaderParam.value() was empty on parameter %s", paramIndex);
                Collection<String> header = addTemplatedParam(data.template().headers()
                        .get(name), name);
                data.template().header(name, header);
                nameParam(data, name, paramIndex);
                isHttpAnnotation = true;
            }

            // TODO
            /*
             * else if (annotationType == FormParam.class) { String name =
			 * FormParam.class.cast(parameterAnnotation).value();
			 * checkState(emptyToNull(name) != null,
			 * "FormParam.value() was empty on parameter %s", paramIndex);
			 * data.formParams().add(name); nameParam(data, name, paramIndex);
			 * isHttpAnnotation = true; }
			 */

        }
        return isHttpAnnotation;
    }

    private <K, V> boolean searchMapValues(Map<K, Collection<V>> map, V search) {
        Collection<Collection<V>> values = map.values();
        if (values == null) {
            return false;
        }
        for (Collection<V> entry : values) {
            if (entry.contains(search)) {
                return true;
            }
        }
        return false;
    }

    private void parseProduces(MethodMetadata md, Method method, RequestMapping annotation) {
        checkAtMostOne(method, annotation.produces(), "produces");
        String[] serverProduces = annotation.produces();
        String clientAccepts = serverProduces.length == 0 ? null
                : emptyToNull(serverProduces[0]);
        if (clientAccepts != null) {
            md.template().header(ACCEPT, clientAccepts);
        }
    }

    private void parseConsumes(MethodMetadata md, Method method, RequestMapping annotation) {
        checkAtMostOne(method, annotation.consumes(), "consumes");
        String[] serverConsumes = annotation.consumes();
        String clientProduces = serverConsumes.length == 0 ? null
                : emptyToNull(serverConsumes[0]);
        if (clientProduces != null) {
            md.template().header(CONTENT_TYPE, clientProduces);
        }
    }

    private void parseParams(MethodMetadata md, Method method, RequestMapping classAnnotation) {
        String[] params = classAnnotation.params();
        for (String param : params) {
            String[] split = param.split("=");
            md.template().query(split[0], split[1]);
        }
    }

    private void parseHeaders(MethodMetadata md, Method method, RequestMapping annotation) {
        // TODO: only supports one header value per key
        if (annotation.headers() != null && annotation.headers().length > 0) {
            for (String header : annotation.headers()) {
                int colon = header.indexOf(':');
                md.template().header(header.substring(0, colon),
                        header.substring(colon + 2));
            }
        }


    }
}
