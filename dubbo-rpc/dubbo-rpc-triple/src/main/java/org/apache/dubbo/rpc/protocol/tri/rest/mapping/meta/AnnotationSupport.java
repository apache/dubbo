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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.meta;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.protocol.tri.rest.util.RestToolKit;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AnnotationSupport {

    private static final AnnotationMeta[] EMPTY = new AnnotationMeta[0];
    private static final int GET_KEY = 1;
    private static final int GET_MERGED_KEY = 2;
    private static final int FIND_KEY = 3;
    private static final int FIND_MERGED_KEY = 4;

    private final Map<Key, Optional<AnnotationMeta>> cache = CollectionUtils.newConcurrentHashMap();
    private final Map<Integer, AnnotationMeta[]> arrayCache = CollectionUtils.newConcurrentHashMap();
    private final RestToolKit toolKit;

    protected AnnotationSupport(RestToolKit toolKit) {
        this.toolKit = toolKit;
    }

    public final AnnotationMeta[] getAnnotations() {
        return arrayCache.computeIfAbsent(GET_KEY, k -> {
            AnnotatedElement element = getAnnotatedElement();
            Annotation[] annotations = element.getAnnotations();
            int len = annotations.length;
            if (len == 0) {
                return EMPTY;
            }
            AnnotationMeta[] metas = new AnnotationMeta[len];
            for (int i = 0; i < len; i++) {
                metas[i] = new AnnotationMeta(element, annotations[i], toolKit);
            }
            return metas;
        });
    }

    public final Annotation[] getRawAnnotations() {
        AnnotationMeta[] annotations = getAnnotations();
        int len = annotations.length;
        Annotation[] result = new Annotation[len];
        for (int i = 0; i < len; i++) {
            result[i] = annotations[i].getAnnotation();
        }
        return result;
    }

    public final <A extends Annotation> AnnotationMeta<A> getAnnotation(Class<A> annotationType) {
        return cache.computeIfAbsent(new Key(annotationType, GET_KEY), k -> {
                    AnnotatedElement element = getAnnotatedElement();
                    Annotation annotation = element.getAnnotation(annotationType);
                    if (annotation != null) {
                        return Optional.of(new AnnotationMeta(element, annotation, toolKit));
                    }
                    return Optional.empty();
                })
                .orElse(null);
    }

    public final AnnotationMeta getAnnotation(AnnotationEnum annotationEnum) {
        return annotationEnum.isPresent() ? getAnnotation(annotationEnum.type()) : null;
    }

    public final boolean isAnnotated(Class<? extends Annotation> annotationType) {
        return getAnnotation(annotationType) != null;
    }

    public final boolean isAnnotated(AnnotationEnum annotationEnum) {
        return getAnnotation(annotationEnum) != null;
    }

    public final <A extends Annotation> AnnotationMeta<A> getMergedAnnotation(Class<A> annotationType) {
        return cache.computeIfAbsent(new Key(annotationType, GET_MERGED_KEY), k -> {
                    AnnotatedElement element = getAnnotatedElement();
                    Annotation[] annotations = element.getAnnotations();
                    for (Annotation annotation : annotations) {
                        if (annotation.annotationType() == annotationType) {
                            return Optional.of(new AnnotationMeta(element, annotation, toolKit));
                        }
                        Annotation metaAnnotation = annotation.annotationType().getAnnotation(annotationType);
                        if (metaAnnotation != null) {
                            return Optional.of(new AnnotationMeta(element, metaAnnotation, toolKit));
                        }
                    }
                    return Optional.empty();
                })
                .orElse(null);
    }

    public final AnnotationMeta getMergedAnnotation(AnnotationEnum annotationEnum) {
        return annotationEnum.isPresent() ? getMergedAnnotation(annotationEnum.type()) : null;
    }

    public final boolean isMergedAnnotated(Class<? extends Annotation> annotationType) {
        return getMergedAnnotation(annotationType) != null;
    }

    public final boolean isMergedAnnotated(AnnotationEnum annotationEnum) {
        return getMergedAnnotation(annotationEnum) != null;
    }

    public final AnnotationMeta[] findAnnotations() {
        return arrayCache.computeIfAbsent(FIND_KEY, k -> {
            List<? extends AnnotatedElement> elements = getAnnotatedElements();
            List<AnnotationMeta> metas = new ArrayList<>();
            for (int i = 0, size = elements.size(); i < size; i++) {
                AnnotatedElement element = elements.get(i);
                Annotation[] annotations = element.getAnnotations();
                for (Annotation annotation : annotations) {
                    metas.add(new AnnotationMeta(element, annotation, toolKit));
                }
            }
            if (metas.isEmpty()) {
                return EMPTY;
            }
            return metas.toArray(new AnnotationMeta[0]);
        });
    }

    public final <A extends Annotation> AnnotationMeta<A> findAnnotation(Class<A> annotationType) {
        return cache.computeIfAbsent(new Key(annotationType, FIND_KEY), k -> {
                    List<? extends AnnotatedElement> elements = getAnnotatedElements();
                    for (int i = 0, size = elements.size(); i < size; i++) {
                        AnnotatedElement element = elements.get(i);
                        Annotation annotation = element.getDeclaredAnnotation(annotationType);
                        if (annotation != null) {
                            return Optional.of(new AnnotationMeta(element, annotation, toolKit));
                        }
                    }
                    return Optional.empty();
                })
                .orElse(null);
    }

    public final AnnotationMeta findAnnotation(AnnotationEnum annotationEnum) {
        return annotationEnum.isPresent() ? findAnnotation(annotationEnum.type()) : null;
    }

    public final boolean isHierarchyAnnotated(Class<? extends Annotation> annotationType) {
        return findAnnotation(annotationType) != null;
    }

    public final boolean isHierarchyAnnotated(AnnotationEnum annotationEnum) {
        return findAnnotation(annotationEnum) != null;
    }

    public final <A extends Annotation> AnnotationMeta<A> findMergedAnnotation(Class<A> annotationType) {
        return cache.computeIfAbsent(new Key(annotationType, FIND_MERGED_KEY), k -> {
                    List<? extends AnnotatedElement> elements = getAnnotatedElements();
                    for (int i = 0, size = elements.size(); i < size; i++) {
                        AnnotatedElement element = elements.get(i);
                        Annotation[] annotations = element.getDeclaredAnnotations();
                        for (Annotation annotation : annotations) {
                            if (annotation.annotationType() == annotationType) {
                                return Optional.of(new AnnotationMeta(element, annotation, toolKit));
                            }
                            Annotation metaAnnotation =
                                    annotation.annotationType().getAnnotation(annotationType);
                            if (metaAnnotation != null) {
                                return Optional.of(new AnnotationMeta(element, metaAnnotation, toolKit));
                            }
                        }
                    }
                    return Optional.empty();
                })
                .orElse(null);
    }

    public final AnnotationMeta findMergedAnnotation(AnnotationEnum annotationEnum) {
        return annotationEnum.isPresent() ? findMergedAnnotation(annotationEnum.type()) : null;
    }

    public final boolean isMergedHierarchyAnnotated(Class<? extends Annotation> annotationType) {
        return findMergedAnnotation(annotationType) != null;
    }

    public final boolean isMergedHierarchyAnnotated(AnnotationEnum annotationEnum) {
        return findMergedAnnotation(annotationEnum) != null;
    }

    public final RestToolKit getToolKit() {
        return toolKit;
    }

    public List<? extends AnnotatedElement> getAnnotatedElements() {
        return Collections.singletonList(getAnnotatedElement());
    }

    private static final class Key {

        private final Class<? extends Annotation> annotationType;
        private final int type;

        Key(Class<? extends Annotation> annotationType, int type) {
            this.annotationType = annotationType;
            this.type = type;
        }

        @Override
        public int hashCode() {
            return (annotationType.hashCode() << 2) + type;
        }

        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "EqualsDoesntCheckParameterClass"})
        @Override
        public boolean equals(Object obj) {
            Key other = (Key) obj;
            return annotationType == other.annotationType && type == other.type;
        }
    }

    protected abstract AnnotatedElement getAnnotatedElement();
}
