/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.protocol.tri.rest.support.basic;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.HttpResponse;
import org.apache.dubbo.remoting.http12.rest.Param;
import org.apache.dubbo.remoting.http12.rest.ParamType;
import org.apache.dubbo.rpc.protocol.tri.rest.Messages;
import org.apache.dubbo.rpc.protocol.tri.rest.RestException;
import org.apache.dubbo.rpc.protocol.tri.rest.argument.CompositeArgumentResolver;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.AnnotationMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.ConstructorMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.NestableParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.BeanMeta.PropertyMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta.ParameterMeta;
import org.apache.dubbo.rpc.protocol.tri.rest.util.TypeUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class BeanArgumentBinder {

    private static final Map<Class<?>, BeanMeta> CACHE = CollectionUtils.newConcurrentHashMap();

    private final CompositeArgumentResolver argumentResolver;

    public BeanArgumentBinder(CompositeArgumentResolver argumentResolver) {
        this.argumentResolver = argumentResolver;
    }

    public Object bind(ParameterMeta paramMeta, HttpRequest request, HttpResponse response) {
        try {
            BeanMeta beanMeta = getBeanMeta(paramMeta);
            if (beanMeta == null) {
                return null;
            }

            ConstructorMeta constructor = beanMeta.getConstructor();
            ParameterMeta[] parameters = constructor.getParameters();
            Object bean;
            int len = parameters.length;
            if (len == 0) {
                bean = constructor.newInstance();
            } else {
                Object[] args = new Object[len];
                for (int i = 0; i < len; i++) {
                    ParameterMeta parameter = parameters[i];
                    args[i] = parameter.isSimple() ? argumentResolver.resolve(parameter, request, response) : null;
                }
                bean = constructor.newInstance(args);
            }

            Node root = new Node(paramMeta.getName(), bean, beanMeta);
            for (String paramName : request.parameterNames()) {
                Node current = root;
                List<String> parts = StringUtils.tokenizeToList(paramName, '.');
                for (int i = 0, size = parts.size(); i < size; i++) {
                    if (current == null) {
                        break;
                    }
                    String name = parts.get(i);
                    Pair<String, String> pair = parseKeyParam(name);
                    if (pair == null) {
                        current = current.getChild(name);
                        if (i == 0 && current == null && name.equals(root.name)) {
                            current = root;
                        }
                    } else {
                        name = pair.getLeft();
                        current = current.getChild(name);
                        if (current == null) {
                            break;
                        }
                        String key = pair.getValue();
                        if (!key.isEmpty()) {
                            if (Character.isDigit(key.charAt(0))) {
                                try {
                                    current = current.getChild(Long.parseLong(key));
                                    continue;
                                } catch (NumberFormatException ignored) {
                                }
                            }
                            current = current.getChild(key);
                        }
                    }
                }
                if (current == null) {
                    continue;
                }
                Class<?> type = current.paramMeta.getActualType();
                Object value;
                if (type.isArray() || Collection.class.isAssignableFrom(type)) {
                    value = request.parameterValues(paramName);
                } else {
                    value = request.parameter(paramName);
                }
                //noinspection unchecked
                current.setValue(argumentResolver.getArgumentConverter().convert(value, current.paramMeta));
            }

            for (PropertyMeta propertyMeta : beanMeta.getProperties()) {
                resolveParam(propertyMeta, bean, request, response);
            }

            return bean;
        } catch (Exception e) {
            throw new RestException(e, Messages.ARGUMENT_BIND_ERROR, paramMeta.getName(), paramMeta.getType());
        }
    }

    private void resolveParam(NestableParameterMeta meta, Object bean, HttpRequest request, HttpResponse response) {
        AnnotationMeta<Param> param = meta.getAnnotation(Param.class);
        if (param == null || param.getAnnotation().type() == ParamType.Param) {
            return;
        }
        meta.setValue(bean, argumentResolver.resolve(meta, request, response));
    }

    private static BeanMeta getBeanMeta(ParameterMeta paramMeta) {
        Class<?> type = paramMeta.getActualType();
        if (paramMeta.isSimple() || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        return CACHE.computeIfAbsent(type, k -> paramMeta.getBeanMeta());
    }

    /**
     * See
     * <p>
     * <a href="https://docs.spring.io/spring-framework/reference/core/validation/beans-beans.html#beans-binding">Spring beans-binding</a>
     */
    private static Pair<String, String> parseKeyParam(String name) {
        int len = name.length();
        if (name.charAt(len - 1) == ']') {
            int start = name.lastIndexOf('[');
            if (start > -1) {
                return Pair.of(name.substring(0, start), name.substring(start + 1, len - 1));
            }
        }
        return null;
    }

    private static final class Node {

        public final String name;
        public NestableParameterMeta paramMeta;
        public Consumer<Object> setter;
        public Object value;
        public Map<Object, Node> children;
        public BeanMeta beanMeta;

        public Node(String name, NestableParameterMeta paramMeta, Consumer<Object> setter) {
            this.name = name;
            this.paramMeta = paramMeta;
            this.setter = setter;
        }

        public Node(String name, Object value, BeanMeta beanMeta) {
            this.name = name;
            this.value = value;
            this.beanMeta = beanMeta;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public Node getChild(String name) {
            if (children != null) {
                Node node = children.get(name);
                if (node != null) {
                    return node;
                }
            }

            if (beanMeta == null) {
                Class<?> type = paramMeta.getType();
                if (Map.class.isAssignableFrom(type)) {
                    return createChild(name, paramMeta.getNestedMeta(), v -> ((Map) value).put(name, v));
                }
                return null;
            }

            PropertyMeta propertyMeta = beanMeta.getProperty(name);
            if (propertyMeta != null) {
                return createChild(name, propertyMeta, v -> propertyMeta.setValue(value, v));
            }

            return null;
        }

        @SuppressWarnings({"rawtypes", "unchecked", "SuspiciousSystemArraycopy"})
        public Node getChild(long num) {
            if (children != null) {
                Node node = children.get(num);
                if (node != null) {
                    return node;
                }
            }

            NestableParameterMeta nestedMeta = paramMeta.getNestedMeta();
            Class<?> type = paramMeta.getType();
            Object childValue = null;
            Node child;
            if (List.class.isAssignableFrom(type)) {
                int index = (int) num;
                List list = (List) value;
                child = new Node(name, nestedMeta, v -> {
                    if (index < 0) {
                        return;
                    }
                    while (list.size() <= index) {
                        list.add(null);
                    }
                    list.set(index, v);
                });
                if (index < list.size()) {
                    childValue = list.get(index);
                }
            } else if (type.isArray()) {
                int index = (int) num;
                int len = Array.getLength(value);
                child = new Node(name, nestedMeta, v -> {
                    if (num >= 0 && num < len) {
                        Array.set(value, index, v);
                        return;
                    }
                    int tail = index < 0 ? len : index;
                    Object newArr = Array.newInstance(value.getClass().getComponentType(), tail + 1);
                    System.arraycopy(value, 0, newArr, 0, len);
                    Array.set(newArr, tail, v);
                    setter.accept(newArr);
                });
                if (index < len) {
                    childValue = Array.get(value, index);
                }
            } else if (Map.class.isAssignableFrom(type)) {
                Class<?> keyType = TypeUtils.getNestedActualType(paramMeta.getGenericType(), 0);
                Object key = TypeUtils.longToObject(num, keyType);
                child = new Node(name, nestedMeta, v -> {
                    ((Map) value).put(key, v);
                });
                childValue = ((Map) value).get(key);
            } else {
                return null;
            }

            if (childValue == null) {
                childValue = createValue(nestedMeta.getType());
                if (childValue == null) {
                    if (nestedMeta.isSimple()) {
                        return child;
                    }
                    BeanMeta beanMeta = getBeanMeta(nestedMeta);
                    if (beanMeta == null) {
                        return null;
                    }
                    child.beanMeta = beanMeta;
                    childValue = beanMeta.newInstance();
                }
                child.setter.accept(childValue);
            } else {
                child.beanMeta = getBeanMeta(nestedMeta);
            }

            putChild(child, num, childValue);
            return child;
        }

        public void setValue(Object value) {
            setter.accept(value);
        }

        private Node createChild(String name, NestableParameterMeta paramMeta, Consumer<Object> setter) {
            Class<?> type = paramMeta.getType();
            if (type.isArray()) {
                Consumer<Object> arraySetter = setter;
                setter = v -> {
                    arraySetter.accept(v);
                    if (children != null) {
                        children.get(name).value = v;
                    }
                };
            }
            Node child = new Node(name, paramMeta, setter);
            Object childValue = paramMeta.getValue(value);
            boolean created = false;
            if (childValue == null) {
                if (value instanceof Map) {
                    childValue = ((Map<?, ?>) value).get(name);
                    if (childValue != null) {
                        child.beanMeta = getBeanMeta(paramMeta);
                    }
                }
                if (childValue == null) {
                    childValue = createValue(type);
                    if (childValue == null) {
                        if (paramMeta.isSimple()) {
                            return child;
                        }
                    } else {
                        created = true;
                    }
                }
            }
            if (childValue == null) {
                BeanMeta beanMeta = getBeanMeta(paramMeta);
                if (beanMeta == null) {
                    return null;
                }
                child.beanMeta = beanMeta;
                childValue = beanMeta.newInstance();
                created = true;
            }
            if (created) {
                setter.accept(childValue);
            }
            putChild(child, name, childValue);
            return child;
        }

        private Object createValue(Class<?> type) {
            if (Map.class.isAssignableFrom(type)) {
                return TypeUtils.createMap(type);
            }
            if (Collection.class.isAssignableFrom(type)) {
                return TypeUtils.createCollection(type);
            }
            if (type.isArray()) {
                return Array.newInstance(type.getComponentType(), 1);
            }
            return null;
        }

        private void putChild(Node child, Object name, Object value) {
            child.value = value;
            if (children == null) {
                children = new HashMap<>();
            }
            children.put(name, child);
        }
    }
}
