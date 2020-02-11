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
package org.apache.dubbo.common.utils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.dubbo.common.function.Predicates.and;
import static org.apache.dubbo.common.function.Streams.filterAll;
import static org.apache.dubbo.common.function.Streams.filterFirst;
import static org.apache.dubbo.common.utils.ClassUtils.getAllInheritedTypes;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;
import static org.apache.dubbo.common.utils.CollectionUtils.first;
import static org.apache.dubbo.common.utils.MethodUtils.invokeMethod;

/**
 * Commons Annotation Utilities class
 *
 * @since 2.7.6
 */
public interface AnnotationUtils {

    /**
     * Resolve the annotation type by the annotated element and resolved class name
     *
     * @param annotatedElement    the annotated element
     * @param annotationClassName the class name of annotation
     * @param <A>                 the type of annotation
     * @return If resolved, return the type of annotation, or <code>null</code>
     */
    static <A extends Annotation> Class<A> resolveAnnotationType(AnnotatedElement annotatedElement,
                                                                 String annotationClassName) {
        ClassLoader classLoader = annotatedElement.getClass().getClassLoader();
        Class<?> annotationType = resolveClass(annotationClassName, classLoader);
        if (annotationType == null || !Annotation.class.isAssignableFrom(annotationType)) {
            return null;
        }
        return (Class<A>) annotationType;
    }

    /**
     * Is the specified type a generic {@link Class type}
     *
     * @param annotatedElement the annotated element
     * @return if <code>annotatedElement</code> is the {@link Class}, return <code>true</code>, or <code>false</code>
     * @see ElementType#TYPE
     */
    static boolean isType(AnnotatedElement annotatedElement) {
        return annotatedElement instanceof Class;
    }

    /**
     * Is the type of specified annotation same to the expected type?
     *
     * @param annotation     the specified {@link Annotation}
     * @param annotationType the expected annotation type
     * @return if same, return <code>true</code>, or <code>false</code>
     */
    static boolean isSameType(Annotation annotation, Class<? extends Annotation> annotationType) {
        if (annotation == null || annotationType == null) {
            return false;
        }
        return Objects.equals(annotation.annotationType(), annotationType);
    }

    /**
     * Build an instance of {@link Predicate} to excluded annotation type
     *
     * @param excludedAnnotationType excluded annotation type
     * @return non-null
     */
    static Predicate<Annotation> excludedType(Class<? extends Annotation> excludedAnnotationType) {
        return annotation -> !isSameType(annotation, excludedAnnotationType);
    }

    /**
     * Get the attribute from the specified {@link Annotation annotation}
     *
     * @param annotation    the specified {@link Annotation annotation}
     * @param attributeName the attribute name
     * @param <T>           the type of attribute
     * @return the attribute value
     * @throws IllegalArgumentException If the attribute name can't be found
     */
    static <T> T getAttribute(Annotation annotation, String attributeName) throws IllegalArgumentException {
        return annotation == null ? null : invokeMethod(annotation, attributeName);
    }

    /**
     * Get the "value" attribute from the specified {@link Annotation annotation}
     *
     * @param annotation the specified {@link Annotation annotation}
     * @param <T>        the type of attribute
     * @return the value of "value" attribute
     * @throws IllegalArgumentException If the attribute name can't be found
     */
    static <T> T getValue(Annotation annotation) throws IllegalArgumentException {
        return getAttribute(annotation, "value");
    }

    /**
     * Get the {@link Annotation} from the specified {@link AnnotatedElement the annotated element} and
     * {@link Annotation annotation} class name
     *
     * @param annotatedElement    {@link AnnotatedElement}
     * @param annotationClassName the class name of annotation
     * @param <A>                 The type of {@link Annotation}
     * @return the {@link Annotation} if found
     * @throws ClassCastException If the {@link Annotation annotation} type that client requires can't match actual type
     */
    static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, String annotationClassName)
            throws ClassCastException {
        Class<? extends Annotation> annotationType = resolveAnnotationType(annotatedElement, annotationClassName);
        if (annotationType == null) {
            return null;
        }
        return (A) annotatedElement.getAnnotation(annotationType);
    }

    /**
     * Get annotations that are <em>directly present</em> on this element.
     * This method ignores inherited annotations.
     *
     * @param annotatedElement    the annotated element
     * @param annotationsToFilter the annotations to filter
     * @return non-null read-only {@link List}
     */
    static List<Annotation> getDeclaredAnnotations(AnnotatedElement annotatedElement,
                                                   Predicate<Annotation>... annotationsToFilter) {
        if (annotatedElement == null) {
            return emptyList();
        }

        return unmodifiableList(filterAll(asList(annotatedElement.getDeclaredAnnotations()), annotationsToFilter));
    }

    /**
     * Get all directly declared annotations of the the annotated element, not including
     * meta annotations.
     *
     * @param annotatedElement    the annotated element
     * @param annotationsToFilter the annotations to filter
     * @return non-null read-only {@link List}
     */
    static List<Annotation> getAllDeclaredAnnotations(AnnotatedElement annotatedElement,
                                                      Predicate<Annotation>... annotationsToFilter) {
        if (isType(annotatedElement)) {
            return getAllDeclaredAnnotations((Class) annotatedElement, annotationsToFilter);
        } else {
            return getDeclaredAnnotations(annotatedElement, annotationsToFilter);
        }
    }

    /**
     * Get all directly declared annotations of the specified type and its' all hierarchical types, not including
     * meta annotations.
     *
     * @param type                the specified type
     * @param annotationsToFilter the annotations to filter
     * @return non-null read-only {@link List}
     */
    static List<Annotation> getAllDeclaredAnnotations(Class<?> type, Predicate<Annotation>... annotationsToFilter) {

        if (type == null) {
            return emptyList();
        }

        List<Annotation> allAnnotations = new LinkedList<>();

        // All types
        Set<Class<?>> allTypes = new LinkedHashSet<>();
        // Add current type
        allTypes.add(type);
        // Add all inherited types
        allTypes.addAll(getAllInheritedTypes(type, t -> !Object.class.equals(t)));

        for (Class<?> t : allTypes) {
            allAnnotations.addAll(getDeclaredAnnotations(t, annotationsToFilter));
        }

        return unmodifiableList(allAnnotations);
    }


    /**
     * Get the meta-annotated {@link Annotation annotations} directly, excluding {@link Target}, {@link Retention}
     * and {@link Documented}
     *
     * @param annotationType          the {@link Annotation annotation} type
     * @param metaAnnotationsToFilter the meta annotations to filter
     * @return non-null read-only {@link List}
     */
    static List<Annotation> getMetaAnnotations(Class<? extends Annotation> annotationType,
                                               Predicate<Annotation>... metaAnnotationsToFilter) {
        return getDeclaredAnnotations(annotationType,
                // Excludes the Java native annotation types or it causes the stack overflow, e.g,
                // @Target annotates itself
                excludedType(Target.class),
                excludedType(Retention.class),
                excludedType(Documented.class),
                // Add other predicates
                and(metaAnnotationsToFilter)
        );
    }

    /**
     * Get all meta annotations from the specified {@link Annotation annotation} type
     *
     * @param annotationType      the {@link Annotation annotation} type
     * @param annotationsToFilter the annotations to filter
     * @return non-null read-only {@link List}
     */
    static List<Annotation> getAllMetaAnnotations(Class<? extends Annotation> annotationType,
                                                  Predicate<Annotation>... annotationsToFilter) {

        List<Annotation> allMetaAnnotations = new LinkedList<>();

        List<Annotation> metaAnnotations = getMetaAnnotations(annotationType);

        allMetaAnnotations.addAll(metaAnnotations);

        for (Annotation metaAnnotation : metaAnnotations) {
            // Get the nested meta annotations recursively
            allMetaAnnotations.addAll(getAllMetaAnnotations(metaAnnotation.annotationType()));
        }

        return unmodifiableList(filterAll(allMetaAnnotations, annotationsToFilter));
    }

    /**
     * Find the annotation that is annotated on the specified element may be a meta-annotation
     *
     * @param annotatedElement    the annotated element
     * @param annotationClassName the class name of annotation
     * @param <A>                 the required type of annotation
     * @return If found, return first matched-type {@link Annotation annotation}, or <code>null</code>
     */
    static <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, String annotationClassName) {
        return findAnnotation(annotatedElement, resolveAnnotationType(annotatedElement, annotationClassName));
    }

    /**
     * Find the annotation that is annotated on the specified element may be a meta-annotation
     *
     * @param annotatedElement the annotated element
     * @param annotationType   the type of annotation
     * @param <A>              the required type of annotation
     * @return If found, return first matched-type {@link Annotation annotation}, or <code>null</code>
     */
    static <A extends Annotation> A findAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        return (A) filterFirst(getAllDeclaredAnnotations(annotatedElement), a -> isSameType(a, annotationType));
    }

    /**
     * Find the meta annotations from the the {@link Annotation annotation} type by meta annotation type
     *
     * @param annotationType     the {@link Annotation annotation} type
     * @param metaAnnotationType the meta annotation type
     * @param <A>                the type of required annotation
     * @return if found, return all matched results, or get an {@link Collections#emptyList() empty list}
     */
    static <A extends Annotation> List<A> findMetaAnnotations(Class<? extends Annotation> annotationType,
                                                              Class<A> metaAnnotationType) {
        return (List<A>) getAllMetaAnnotations(annotationType, a -> isSameType(a, metaAnnotationType));
    }

    /**
     * Find the meta annotations from the the the annotated element by meta annotation type
     *
     * @param annotatedElement   the annotated element
     * @param metaAnnotationType the meta annotation type
     * @param <A>                the type of required annotation
     * @return if found, return all matched results, or get an {@link Collections#emptyList() empty list}
     */
    static <A extends Annotation> List<A> findMetaAnnotations(AnnotatedElement annotatedElement,
                                                              Class<A> metaAnnotationType) {
        List<A> metaAnnotations = new LinkedList<>();

        for (Annotation annotation : getAllDeclaredAnnotations(annotatedElement)) {
            metaAnnotations.addAll(findMetaAnnotations(annotation.annotationType(), metaAnnotationType));
        }

        return unmodifiableList(metaAnnotations);
    }

    /**
     * Find the meta annotation from the annotated element by meta annotation type
     *
     * @param annotatedElement        the annotated element
     * @param metaAnnotationClassName the class name of meta annotation
     * @param <A>                     the type of required annotation
     * @return {@link #findMetaAnnotation(Class, Class)}
     */
    static <A extends Annotation> A findMetaAnnotation(AnnotatedElement annotatedElement,
                                                       String metaAnnotationClassName) {
        return findMetaAnnotation(annotatedElement, resolveAnnotationType(annotatedElement, metaAnnotationClassName));
    }

    /**
     * Find the meta annotation from the annotation type by meta annotation type
     *
     * @param annotationType     the {@link Annotation annotation} type
     * @param metaAnnotationType the meta annotation type
     * @param <A>                the type of required annotation
     * @return If found, return the {@link CollectionUtils#first(Collection)} matched result, return <code>null</code>.
     * If it requires more result, please consider to use {@link #findMetaAnnotations(Class, Class)}
     * @see #findMetaAnnotations(Class, Class)
     */
    static <A extends Annotation> A findMetaAnnotation(Class<? extends Annotation> annotationType,
                                                       Class<A> metaAnnotationType) {
        return first(findMetaAnnotations(annotationType, metaAnnotationType));
    }

    /**
     * Find the meta annotation from the annotated element by meta annotation type
     *
     * @param annotatedElement   the annotated element
     * @param metaAnnotationType the meta annotation type
     * @param <A>                the type of required annotation
     * @return If found, return the {@link CollectionUtils#first(Collection)} matched result, return <code>null</code>.
     * If it requires more result, please consider to use {@link #findMetaAnnotations(AnnotatedElement, Class)}
     * @see #findMetaAnnotations(AnnotatedElement, Class)
     */
    static <A extends Annotation> A findMetaAnnotation(AnnotatedElement annotatedElement, Class<A> metaAnnotationType) {
        return first(findMetaAnnotations(annotatedElement, metaAnnotationType));
    }

    /**
     * Tests the annotated element is annotated the specified annotations or not
     *
     * @param type            the annotated type
     * @param matchAll        If <code>true</code>, checking all annotation types are present or not, or match any
     * @param annotationTypes the specified annotation types
     * @return If the specified annotation types are present, return <code>true</code>, or <code>false</code>
     */
    static boolean isAnnotationPresent(Class<?> type,
                                       boolean matchAll,
                                       Class<? extends Annotation>... annotationTypes) {

        int size = annotationTypes == null ? 0 : annotationTypes.length;

        if (size < 1) {
            return false;
        }

        int presentCount = 0;

        for (int i = 0; i < size; i++) {
            Class<? extends Annotation> annotationType = annotationTypes[i];
            if (findAnnotation(type, annotationType) != null || findMetaAnnotation(type, annotationType) != null) {
                presentCount++;
            }
        }

        return matchAll ? presentCount == size : presentCount > 0;
    }

    /**
     * Tests the annotated element is annotated the specified annotation or not
     *
     * @param type           the annotated type
     * @param annotationType the class of annotation
     * @return If the specified annotation type is present, return <code>true</code>, or <code>false</code>
     */
    static boolean isAnnotationPresent(Class<?> type, Class<? extends Annotation> annotationType) {
        return isAnnotationPresent(type, true, annotationType);
    }

    /**
     * Tests the annotated element is present any specified annotation types
     *
     * @param annotatedElement    the annotated element
     * @param annotationClassName the class name of annotation
     * @return If any specified annotation types are present, return <code>true</code>
     */
    static boolean isAnnotationPresent(AnnotatedElement annotatedElement, String annotationClassName) {
        ClassLoader classLoader = annotatedElement.getClass().getClassLoader();
        Class<?> resolvedType = resolveClass(annotationClassName, classLoader);
        if (!Annotation.class.isAssignableFrom(resolvedType)) {
            return false;
        }
        return isAnnotationPresent(annotatedElement, (Class<? extends Annotation>) resolvedType);
    }

    /**
     * Tests the annotated element is present any specified annotation types
     *
     * @param annotatedElement the annotated element
     * @param annotationType   the class of annotation
     * @return If any specified annotation types are present, return <code>true</code>
     */
    static boolean isAnnotationPresent(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationType) {
        if (isType(annotatedElement)) {
            return isAnnotationPresent((Class) annotatedElement, annotationType);
        } else {
            return annotatedElement.isAnnotationPresent(annotationType) ||
                    findMetaAnnotation(annotatedElement, annotationType) != null; // to find meta-annotation
        }
    }

    /**
     * Tests the annotated element is annotated all specified annotations or not
     *
     * @param type            the annotated type
     * @param annotationTypes the specified annotation types
     * @return If the specified annotation types are present, return <code>true</code>, or <code>false</code>
     */
    static boolean isAllAnnotationPresent(Class<?> type, Class<? extends Annotation>... annotationTypes) {
        return isAnnotationPresent(type, true, annotationTypes);
    }

    /**
     * Tests the annotated element is present any specified annotation types
     *
     * @param type            the annotated type
     * @param annotationTypes the specified annotation types
     * @return If any specified annotation types are present, return <code>true</code>
     */
    static boolean isAnyAnnotationPresent(Class<?> type,
                                          Class<? extends Annotation>... annotationTypes) {
        return isAnnotationPresent(type, false, annotationTypes);
    }
}
