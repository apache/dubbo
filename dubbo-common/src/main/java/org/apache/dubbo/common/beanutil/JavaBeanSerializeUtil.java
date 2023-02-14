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
package org.apache.dubbo.common.beanutil;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.DefaultSerializeClassChecker;
import org.apache.dubbo.common.utils.LogHelper;
import org.apache.dubbo.common.utils.ReflectUtils;

public final class JavaBeanSerializeUtil {

    private static final Logger logger = LoggerFactory.getLogger(JavaBeanSerializeUtil.class);
    private static final Map<String, Class<?>> TYPES = new HashMap<String, Class<?>>();
    private static final String ARRAY_PREFIX = "[";
    private static final String REFERENCE_TYPE_PREFIX = "L";
    private static final String REFERENCE_TYPE_SUFFIX = ";";

    static {
        TYPES.put(boolean.class.getName(), boolean.class);
        TYPES.put(byte.class.getName(), byte.class);
        TYPES.put(short.class.getName(), short.class);
        TYPES.put(int.class.getName(), int.class);
        TYPES.put(long.class.getName(), long.class);
        TYPES.put(float.class.getName(), float.class);
        TYPES.put(double.class.getName(), double.class);
        TYPES.put(void.class.getName(), void.class);
        TYPES.put("Z", boolean.class);
        TYPES.put("B", byte.class);
        TYPES.put("C", char.class);
        TYPES.put("D", double.class);
        TYPES.put("F", float.class);
        TYPES.put("I", int.class);
        TYPES.put("J", long.class);
        TYPES.put("S", short.class);
    }

    private JavaBeanSerializeUtil() {
    }

    public static JavaBeanDescriptor serialize(Object obj) {
        return serialize(obj, JavaBeanAccessor.FIELD);
    }

    public static JavaBeanDescriptor serialize(Object obj, JavaBeanAccessor accessor) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof JavaBeanDescriptor) {
            return (JavaBeanDescriptor) obj;
        }
        IdentityHashMap<Object, JavaBeanDescriptor> cache = new IdentityHashMap<Object, JavaBeanDescriptor>();
        return createDescriptorIfAbsent(obj, accessor, cache);
    }

    private static JavaBeanDescriptor createDescriptorForSerialize(Class<?> cl) {
        if (cl.isEnum()) {
            return new JavaBeanDescriptor(cl.getName(), JavaBeanDescriptor.TYPE_ENUM);
        }

        if (cl.isArray()) {
            return new JavaBeanDescriptor(cl.getComponentType().getName(), JavaBeanDescriptor.TYPE_ARRAY);
        }

        if (ReflectUtils.isPrimitive(cl)) {
            return new JavaBeanDescriptor(cl.getName(), JavaBeanDescriptor.TYPE_PRIMITIVE);
        }

        if (Class.class.equals(cl)) {
            return new JavaBeanDescriptor(Class.class.getName(), JavaBeanDescriptor.TYPE_CLASS);
        }

        if (Collection.class.isAssignableFrom(cl)) {
            return new JavaBeanDescriptor(cl.getName(), JavaBeanDescriptor.TYPE_COLLECTION);
        }

        if (Map.class.isAssignableFrom(cl)) {
            return new JavaBeanDescriptor(cl.getName(), JavaBeanDescriptor.TYPE_MAP);
        }

        return new JavaBeanDescriptor(cl.getName(), JavaBeanDescriptor.TYPE_BEAN);
    }

    private static JavaBeanDescriptor createDescriptorIfAbsent(Object obj, JavaBeanAccessor accessor,
                                                               IdentityHashMap<Object, JavaBeanDescriptor> cache) {
        if (cache.containsKey(obj)) {
            return cache.get(obj);
        }

        if (obj instanceof JavaBeanDescriptor) {
            return (JavaBeanDescriptor) obj;
        }

        JavaBeanDescriptor result = createDescriptorForSerialize(obj.getClass());
        cache.put(obj, result);
        serializeInternal(result, obj, accessor, cache);
        return result;
    }

    private static void serializeInternal(JavaBeanDescriptor descriptor, Object obj, JavaBeanAccessor accessor,
                                          IdentityHashMap<Object, JavaBeanDescriptor> cache) {
        if (obj == null || descriptor == null) {
            return;
        }

        if (obj.getClass().isEnum()) {
            descriptor.setEnumNameProperty(((Enum<?>) obj).name());
        } else if (ReflectUtils.isPrimitive(obj.getClass())) {
            descriptor.setPrimitiveProperty(obj);
        } else if (Class.class.equals(obj.getClass())) {
            descriptor.setClassNameProperty(((Class<?>) obj).getName());
        } else if (obj.getClass().isArray()) {
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(obj, i);
                if (item == null) {
                    descriptor.setProperty(i, null);
                } else {
                    JavaBeanDescriptor itemDescriptor = createDescriptorIfAbsent(item, accessor, cache);
                    descriptor.setProperty(i, itemDescriptor);
                }
            }
        } else if (obj instanceof Collection) {
            Collection collection = (Collection) obj;
            int index = 0;
            for (Object item : collection) {
                if (item == null) {
                    descriptor.setProperty(index++, null);
                } else {
                    JavaBeanDescriptor itemDescriptor = createDescriptorIfAbsent(item, accessor, cache);
                    descriptor.setProperty(index++, itemDescriptor);
                }
            }
        } else if (obj instanceof Map) {
            Map map = (Map) obj;
            map.forEach((key, value) -> {
                Object keyDescriptor = key == null ? null : createDescriptorIfAbsent(key, accessor, cache);
                Object valueDescriptor = value == null ? null : createDescriptorIfAbsent(value, accessor, cache);
                descriptor.setProperty(keyDescriptor, valueDescriptor);
            });// ~ end of loop map
        } else {
            if (JavaBeanAccessor.isAccessByMethod(accessor)) {
                Map<String, Method> methods = ReflectUtils.getBeanPropertyReadMethods(obj.getClass());
                for (Map.Entry<String, Method> entry : methods.entrySet()) {
                    try {
                        Object value = entry.getValue().invoke(obj);
                        if (value == null) {
                            continue;
                        }
                        JavaBeanDescriptor valueDescriptor = createDescriptorIfAbsent(value, accessor, cache);
                        descriptor.setProperty(entry.getKey(), valueDescriptor);
                    } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                } // ~ end of loop method map
            } // ~ end of if (JavaBeanAccessor.isAccessByMethod(accessor))

            if (JavaBeanAccessor.isAccessByField(accessor)) {
                Map<String, Field> fields = ReflectUtils.getBeanPropertyFields(obj.getClass());
                for (Map.Entry<String, Field> entry : fields.entrySet()) {
                    if (!descriptor.containsProperty(entry.getKey())) {
                        try {
                            Object value = entry.getValue().get(obj);
                            if (value == null) {
                                continue;
                            }
                            JavaBeanDescriptor valueDescriptor = createDescriptorIfAbsent(value, accessor, cache);
                            descriptor.setProperty(entry.getKey(), valueDescriptor);
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }
                } // ~ end of loop field map
            } // ~ end of if (JavaBeanAccessor.isAccessByField(accessor))

        } // ~ end of else

    } // ~ end of method serializeInternal

    public static Object deserialize(JavaBeanDescriptor beanDescriptor) {
        return deserialize(
                beanDescriptor,
                Thread.currentThread().getContextClassLoader());
    }

    public static Object deserialize(JavaBeanDescriptor beanDescriptor, ClassLoader loader) {
        if (beanDescriptor == null) {
            return null;
        }
        IdentityHashMap<JavaBeanDescriptor, Object> cache = new IdentityHashMap<JavaBeanDescriptor, Object>();
        Object result = instantiateForDeserialize(beanDescriptor, loader, cache);
        deserializeInternal(result, beanDescriptor, loader, cache);
        return result;
    }

    private static void deserializeInternal(Object result, JavaBeanDescriptor beanDescriptor, ClassLoader loader,
                                            IdentityHashMap<JavaBeanDescriptor, Object> cache) {
        if (beanDescriptor.isEnumType() || beanDescriptor.isClassType() || beanDescriptor.isPrimitiveType()) {
            return;
        }

        if (beanDescriptor.isArrayType()) {
            int index = 0;
            for (Map.Entry<Object, Object> entry : beanDescriptor) {
                Object item = entry.getValue();
                if (item instanceof JavaBeanDescriptor) {
                    JavaBeanDescriptor itemDescriptor = (JavaBeanDescriptor) entry.getValue();
                    item = instantiateForDeserialize(itemDescriptor, loader, cache);
                    deserializeInternal(item, itemDescriptor, loader, cache);
                }
                Array.set(result, index++, item);
            }
        } else if (beanDescriptor.isCollectionType()) {
            Collection collection = (Collection) result;
            for (Map.Entry<Object, Object> entry : beanDescriptor) {
                Object item = entry.getValue();
                if (item instanceof JavaBeanDescriptor) {
                    JavaBeanDescriptor itemDescriptor = (JavaBeanDescriptor) entry.getValue();
                    item = instantiateForDeserialize(itemDescriptor, loader, cache);
                    deserializeInternal(item, itemDescriptor, loader, cache);
                }
                collection.add(item);
            }
        } else if (beanDescriptor.isMapType()) {
            Map map = (Map) result;
            for (Map.Entry<Object, Object> entry : beanDescriptor) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                if (key instanceof JavaBeanDescriptor) {
                    JavaBeanDescriptor keyDescriptor = (JavaBeanDescriptor) entry.getKey();
                    key = instantiateForDeserialize(keyDescriptor, loader, cache);
                    deserializeInternal(key, keyDescriptor, loader, cache);
                }
                if (value instanceof JavaBeanDescriptor) {
                    JavaBeanDescriptor valueDescriptor = (JavaBeanDescriptor) entry.getValue();
                    value = instantiateForDeserialize(valueDescriptor, loader, cache);
                    deserializeInternal(value, valueDescriptor, loader, cache);
                }
                map.put(key, value);
            }
        } else if (beanDescriptor.isBeanType()) {
            for (Map.Entry<Object, Object> entry : beanDescriptor) {
                String property = entry.getKey().toString();
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }

                if (value instanceof JavaBeanDescriptor) {
                    JavaBeanDescriptor valueDescriptor = (JavaBeanDescriptor) entry.getValue();
                    value = instantiateForDeserialize(valueDescriptor, loader, cache);
                    deserializeInternal(value, valueDescriptor, loader, cache);
                }

                Method method = getSetterMethod(result.getClass(), property, value.getClass());
                boolean setByMethod = false;
                try {
                    if (method != null) {
                        method.invoke(result, value);
                        setByMethod = true;
                    }
                } catch (Exception e) {
                    LogHelper.warn(logger, "Failed to set property through method " + method, e);
                }

                if (!setByMethod) {
                    try {
                        Field field = result.getClass().getField(property);
                        if (field != null) {
                            field.set(result, value);
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e1) {
                        LogHelper.warn(logger, "Failed to set field value", e1);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported type " +
                    beanDescriptor.getClassName() +
                    ":" + beanDescriptor.getType());
        }
    }

    private static Method getSetterMethod(Class<?> cls, String property, Class<?> valueCls) {
        String name = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
        Method method = null;
        try {
            method = cls.getMethod(name, valueCls);
        } catch (NoSuchMethodException e) {
            for (Method m : cls.getMethods()) {
                if (ReflectUtils.isBeanPropertyWriteMethod(m)
                        && m.getName().equals(name)) {
                    method = m;
                }
            }
        }
        if (method != null) {
            method.setAccessible(true);
        }
        return method;
    }

    private static Object instantiate(Class<?> cl) throws Exception {
        Constructor<?>[] constructors = cl.getDeclaredConstructors();
        Constructor<?> constructor = null;
        int argc = Integer.MAX_VALUE;
        for (Constructor<?> c : constructors) {
            if (c.getParameterTypes().length < argc) {
                argc = c.getParameterTypes().length;
                constructor = c;
            }
        }

        if (constructor != null) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] constructorArgs = new Object[paramTypes.length];
            for (int i = 0; i < constructorArgs.length; i++) {
                constructorArgs[i] = getConstructorArg(paramTypes[i]);
            }
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(constructorArgs);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LogHelper.warn(logger, e.getMessage(), e);
            }
        }

        return cl.getDeclaredConstructor().newInstance();
    }

    public static Object getConstructorArg(Class<?> cl) {
        if (boolean.class.equals(cl) || Boolean.class.equals(cl)) {
            return Boolean.FALSE;
        }

        if (byte.class.equals(cl) || Byte.class.equals(cl)) {
            return (byte) 0;
        }

        if (short.class.equals(cl) || Short.class.equals(cl)) {
            return (short) 0;
        }

        if (int.class.equals(cl) || Integer.class.equals(cl)) {
            return 0;
        }

        if (long.class.equals(cl) || Long.class.equals(cl)) {
            return 0L;
        }

        if (float.class.equals(cl) || Float.class.equals(cl)) {
            return (float) 0;
        }

        if (double.class.equals(cl) || Double.class.equals(cl)) {
            return (double) 0;
        }

        if (char.class.equals(cl) || Character.class.equals(cl)) {
            return (char) 0;
        }
        return null;
    }

    private static Object instantiateForDeserialize(JavaBeanDescriptor beanDescriptor, ClassLoader loader,
                                                    IdentityHashMap<JavaBeanDescriptor, Object> cache) {
        if (cache.containsKey(beanDescriptor)) {
            return cache.get(beanDescriptor);
        }

        if (beanDescriptor.isClassType()) {
            try {
                return name2Class(loader, beanDescriptor.getClassNameProperty());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        if (beanDescriptor.isEnumType()) {
            try {
                Class<?> enumType = name2Class(loader, beanDescriptor.getClassName());
                Method method = getEnumValueOfMethod(enumType);
                return method.invoke(null, enumType, beanDescriptor.getEnumPropertyName());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        if (beanDescriptor.isPrimitiveType()) {
            return beanDescriptor.getPrimitiveProperty();
        }

        Object result;
        if (beanDescriptor.isArrayType()) {
            Class<?> componentType;
            try {
                componentType = name2Class(loader, beanDescriptor.getClassName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            result = Array.newInstance(componentType, beanDescriptor.propertySize());
            cache.put(beanDescriptor, result);
        } else {
            try {
                Class<?> cl = name2Class(loader, beanDescriptor.getClassName());
                result = instantiate(cl);
                cache.put(beanDescriptor, result);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        return result;
    }

    /**
     * Transform the Class.forName String to Class Object.
     *
     * @param name Class.getName()
     * @return Class
     * @throws ClassNotFoundException Class.forName
     */
    public static Class<?> name2Class(ClassLoader loader, String name) throws ClassNotFoundException {
        if (TYPES.containsKey(name)) {
            return TYPES.get(name);
        }
        if (isArray(name)) {
            int dimension = 0;
            while (isArray(name)) {
                ++dimension;
                name = name.substring(1);
            }
            Class type = name2Class(loader, name);
            int[] dimensions = new int[dimension];
            for (int i = 0; i < dimension; i++) {
                dimensions[i] = 0;
            }
            return Array.newInstance(type, dimensions).getClass();
        }
        if (isReferenceType(name)) {
            name = name.substring(1, name.length() - 1);
        }
        return DefaultSerializeClassChecker.getInstance().loadClass(loader, name);
    }

    private static boolean isArray(String type) {
        return type != null && type.startsWith(ARRAY_PREFIX);
    }

    private static boolean isReferenceType(String type) {
        return type != null
                && type.startsWith(REFERENCE_TYPE_PREFIX)
                && type.endsWith(REFERENCE_TYPE_SUFFIX);
    }

    private static Method getEnumValueOfMethod(Class cl) throws NoSuchMethodException {
        return cl.getMethod("valueOf", Class.class, String.class);
    }

}
