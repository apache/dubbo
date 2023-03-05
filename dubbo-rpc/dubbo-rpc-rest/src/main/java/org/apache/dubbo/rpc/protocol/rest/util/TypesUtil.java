
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
package org.apache.dubbo.rpc.protocol.rest.util;


import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;



public class TypesUtil {


    public static Class<?> getRawType(Type type) {
        if (type instanceof Class<?>) {
            // type is a normal class.
            return (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            return (Class<?>) rawType;
        } else if (type instanceof GenericArrayType) {
            final GenericArrayType genericArrayType = (GenericArrayType) type;
            final Class<?> componentRawType = getRawType(genericArrayType.getGenericComponentType());
            return Array.newInstance(componentRawType, 0).getClass();
        } else if (type instanceof TypeVariable) {
            final TypeVariable typeVar = (TypeVariable) type;
            if (typeVar.getBounds() != null && typeVar.getBounds().length > 0) {
                return getRawType(typeVar.getBounds()[0]);
            }
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            Type[] upperBounds = wildcardType.getUpperBounds();
            if (upperBounds != null && upperBounds.length > 0) {
                return getRawType(upperBounds[0]);
            }
        }
        throw new RuntimeException("unableToDetermineBaseClass");
    }



    /**
     * Given a class and an interfaces, go through the class hierarchy to find the interface and return its type arguments.
     *
     * @param classToSearch   class
     * @param interfaceToFind interface to find
     * @return type arguments of the interface
     */
    public static Type[] getActualTypeArgumentsOfAnInterface(Class<?> classToSearch, Class<?> interfaceToFind) {
        Type[] types = findParameterizedTypes(classToSearch, interfaceToFind);
        return types;
    }

    private static final Type[] EMPTY_TYPE_ARRAY = {};

    /**
     * Search for the given interface or class within the root's class/interface hierarchy.
     * If the searched for class/interface is a generic return an array of real types that fill it out.
     *
     * @param root        root class
     * @param searchedFor searched class
     * @return for generic class/interface returns array of real types
     */
    public static Type[] findParameterizedTypes(Class<?> root, Class<?> searchedFor) {
        if (searchedFor.isInterface()) {
            return findInterfaceParameterizedTypes(root, null, searchedFor);
        }
        return findClassParameterizedTypes(root, null, searchedFor);
    }

    public static Type[] findClassParameterizedTypes(Class<?> root, ParameterizedType rootType, Class<?> searchedForClass) {
        if (Object.class.equals(root)) return null;

        Map<TypeVariable<?>, Type> typeVarMap = populateParameterizedMap(root, rootType);

        Class<?> superclass = root.getSuperclass();
        Type genericSuper = root.getGenericSuperclass();

        if (superclass.equals(searchedForClass)) {
            return extractTypes(typeVarMap, genericSuper);
        }


        if (genericSuper instanceof ParameterizedType) {
            ParameterizedType intfParam = (ParameterizedType) genericSuper;
            Type[] types = findClassParameterizedTypes(superclass, intfParam, searchedForClass);
            if (types != null) {
                return extractTypeVariables(typeVarMap, types);
            }
        } else {
            Type[] types = findClassParameterizedTypes(superclass, null, searchedForClass);
            if (types != null) {
                return types;
            }
        }
        return null;
    }

    private static Map<TypeVariable<?>, Type> populateParameterizedMap(Class<?> root, ParameterizedType rootType) {
        Map<TypeVariable<?>, Type> typeVarMap = new HashMap<>();
        if (rootType != null) {
            TypeVariable<? extends Class<?>>[] vars = root.getTypeParameters();
            for (int i = 0; i < vars.length; i++) {
                typeVarMap.put(vars[i], rootType.getActualTypeArguments()[i]);
            }
        }
        return typeVarMap;
    }


    public static Type[] findInterfaceParameterizedTypes(Class<?> root, ParameterizedType rootType, Class<?> searchedForInterface) {
        Map<TypeVariable<?>, Type> typeVarMap = populateParameterizedMap(root, rootType);

        for (int i = 0; i < root.getInterfaces().length; i++) {
            Class<?> sub = root.getInterfaces()[i];
            Type genericSub = root.getGenericInterfaces()[i];
            if (sub.equals(searchedForInterface)) {
                return extractTypes(typeVarMap, genericSub);
            }
        }

        for (int i = 0; i < root.getInterfaces().length; i++) {
            Type genericSub = root.getGenericInterfaces()[i];
            Class<?> sub = root.getInterfaces()[i];

            Type[] types = recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSub, sub);
            if (types != null) return types;
        }
        if (root.isInterface()) return null;

        Class<?> superclass = root.getSuperclass();
        Type genericSuper = root.getGenericSuperclass();


        return recurseSuperclassForInterface(searchedForInterface, typeVarMap, genericSuper, superclass);
    }

    private static Type[] recurseSuperclassForInterface(Class<?> searchedForInterface, Map<TypeVariable<?>, Type> typeVarMap, Type genericSub, Class<?> sub) {
        if (genericSub instanceof ParameterizedType) {
            ParameterizedType intfParam = (ParameterizedType) genericSub;
            Type[] types = findInterfaceParameterizedTypes(sub, intfParam, searchedForInterface);
            if (types != null) {
                return extractTypeVariables(typeVarMap, types);
            }
        } else {
            Type[] types = findInterfaceParameterizedTypes(sub, null, searchedForInterface);
            if (types != null) {
                return types;
            }
        }
        return null;
    }

    /**
     * Resolve generic types to actual types.
     *
     * @param typeVarMap The mapping for generic types to actual types.
     * @param types      The types to resolve.
     * @return An array of resolved method parameter types in declaration order.
     */
    private static Type[] extractTypeVariables(final Map<TypeVariable<?>, Type> typeVarMap, final Type[] types) {
        final Type[] resolvedMethodParameterTypes = new Type[types.length];

        for (int i = 0; i < types.length; i++) {
            final Type methodParameterType = types[i];

            if (methodParameterType instanceof TypeVariable<?>) {
                resolvedMethodParameterTypes[i] = typeVarMap.get(methodParameterType);
            } else {
                resolvedMethodParameterTypes[i] = methodParameterType;
            }
        }

        return resolvedMethodParameterTypes;
    }

    private static Type[] extractTypes(Map<TypeVariable<?>, Type> typeVarMap, Type genericSub) {
        if (genericSub instanceof ParameterizedType) {
            ParameterizedType param = (ParameterizedType) genericSub;
            Type[] types = param.getActualTypeArguments();

            Type[] returnTypes = extractTypeVariables(typeVarMap, types);
            return returnTypes;
        } else {
            return EMPTY_TYPE_ARRAY;
        }
    }


}

