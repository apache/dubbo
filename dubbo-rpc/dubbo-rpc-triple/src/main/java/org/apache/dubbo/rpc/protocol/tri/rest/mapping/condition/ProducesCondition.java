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
package org.apache.dubbo.rpc.protocol.tri.rest.mapping.condition;

import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpRequest;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

public final class ProducesCondition implements Condition<ProducesCondition, HttpRequest> {

    private final List<MediaTypeExpression> expressions;

    public ProducesCondition(String... produces) {
        this(produces, null);
    }

    public ProducesCondition(String[] produces, String[] headers) {
        Set<MediaTypeExpression> expressions = null;
        if (headers != null) {
            for (String header : headers) {
                NameValueExpression expr = NameValueExpression.parse(header);
                if (HttpHeaderNames.ACCEPT.getName().equalsIgnoreCase(expr.getName())) {
                    MediaTypeExpression expression = MediaTypeExpression.parse(expr.getValue());
                    if (expression == null) {
                        continue;
                    }
                    if (expressions == null) {
                        expressions = new LinkedHashSet<>();
                    }
                    expressions.add(expression);
                }
            }
        }
        if (produces != null) {
            for (String produce : produces) {
                MediaTypeExpression expression = MediaTypeExpression.parse(produce);
                if (expression == null) {
                    continue;
                }
                if (expressions == null) {
                    expressions = new LinkedHashSet<>();
                }
                expressions.add(expression);
            }
        }
        if (expressions == null) {
            this.expressions = Collections.emptyList();
        } else {
            this.expressions = new ArrayList<>(expressions);
            Collections.sort(this.expressions);
        }
    }

    private ProducesCondition(List<MediaTypeExpression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public ProducesCondition combine(ProducesCondition other) {
        return other.expressions.isEmpty() ? this : other;
    }

    @Override
    public ProducesCondition match(HttpRequest request) {
        if (expressions.isEmpty()) {
            return null;
        }

        List<MediaTypeExpression> acceptedMediaTypes = getAcceptedMediaTypes(request);
        List<MediaTypeExpression> result = null;
        for (int i = 0, size = expressions.size(); i < size; i++) {
            MediaTypeExpression expression = expressions.get(i);
            for (int j = 0, aSize = acceptedMediaTypes.size(); j < aSize; j++) {
                if (expression.compatibleWith(acceptedMediaTypes.get(j))) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    result.add(expression);
                    break;
                }
            }
        }
        return result == null ? null : new ProducesCondition(result);
    }

    private List<MediaTypeExpression> getAcceptedMediaTypes(HttpRequest request) {
        List<String> values = request.headerValues(HttpHeaderNames.ACCEPT.getKey());
        if (CollectionUtils.isEmpty(values)) {
            return MediaTypeExpression.ALL_LIST;
        }
        List<MediaTypeExpression> mediaTypes = null;
        for (int i = 0, size = values.size(); i < size; i++) {
            String value = values.get(i);
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            for (String item : StringUtils.tokenize(value, ',')) {
                MediaTypeExpression expression = MediaTypeExpression.parse(item);
                if (expression == null) {
                    continue;
                }
                if (mediaTypes == null) {
                    mediaTypes = new ArrayList<>();
                }
                mediaTypes.add(expression);
            }
        }
        if (mediaTypes == null) {
            return Collections.emptyList();
        }
        mediaTypes.sort(MediaTypeExpression.QUALITY_COMPARATOR.thenComparing(MediaTypeExpression.COMPARATOR));
        return mediaTypes;
    }

    @Override
    public int compareTo(ProducesCondition other, HttpRequest request) {
        if (expressions.isEmpty() && other.expressions.isEmpty()) {
            return 0;
        }
        List<MediaTypeExpression> mediaTypes = getAcceptedMediaTypes(request);
        for (int i = 0, size = mediaTypes.size(); i < size; i++) {
            MediaTypeExpression mediaType = mediaTypes.get(i);
            Pair<Integer, MediaTypeExpression> thisPair, otherPair;

            thisPair = findMediaType(mediaType, MediaTypeExpression::typesEquals);
            otherPair = findMediaType(mediaType, MediaTypeExpression::typesEquals);
            int result = compareMediaType(thisPair, otherPair);
            if (result != 0) {
                return result;
            }

            thisPair = findMediaType(mediaType, MediaTypeExpression::compatibleWith);
            otherPair = findMediaType(mediaType, MediaTypeExpression::compatibleWith);
            result = compareMediaType(thisPair, otherPair);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    public List<MediaType> getMediaTypes() {
        return MediaTypeExpression.toMediaTypes(expressions);
    }

    @Override
    public int hashCode() {
        return expressions.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != ProducesCondition.class) {
            return false;
        }
        return expressions.equals(((ProducesCondition) obj).expressions);
    }

    private Pair<Integer, MediaTypeExpression> findMediaType(
            MediaTypeExpression mediaType, BiPredicate<MediaTypeExpression, MediaTypeExpression> tester) {
        List<MediaTypeExpression> toCompare = expressions.isEmpty() ? MediaTypeExpression.ALL_LIST : expressions;
        for (int i = 0; i < toCompare.size(); i++) {
            MediaTypeExpression currentMediaType = toCompare.get(i);
            if (tester.test(mediaType, currentMediaType)) {
                return Pair.of(i, currentMediaType);
            }
        }
        return Pair.of(-1, null);
    }

    private int compareMediaType(Pair<Integer, MediaTypeExpression> p1, Pair<Integer, MediaTypeExpression> p2) {
        int index1 = p1.getLeft();
        int index2 = p2.getLeft();
        if (index1 != index2) {
            return index2 - index1;
        }
        return index1 != -1 ? p1.getRight().compareTo(p2.getRight()) : 0;
    }

    @Override
    public String toString() {
        return "ProducesCondition{mediaTypes=" + expressions + '}';
    }
}
