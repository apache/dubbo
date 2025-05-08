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

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.http12.HttpUtils;
import org.apache.dubbo.remoting.http12.message.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class MediaTypeExpression implements Comparable<MediaTypeExpression> {

    public static final MediaTypeExpression ALL = new MediaTypeExpression(MediaType.WILDCARD, MediaType.WILDCARD);
    public static final List<MediaTypeExpression> ALL_LIST = Collections.singletonList(ALL);

    public static final Comparator<MediaTypeExpression> COMPARATOR = (m1, m2) -> {
        int comparison = compareQuality(m1, m2);
        if (comparison != 0) {
            return comparison;
        }

        comparison = compareType(m1.type, m2.type);
        if (comparison != Integer.MIN_VALUE) {
            return comparison;
        }

        comparison = compareType(m1.subType, m2.subType);
        return comparison == Integer.MIN_VALUE ? 0 : comparison;
    };

    public static final Comparator<MediaTypeExpression> QUALITY_COMPARATOR = MediaTypeExpression::compareQuality;

    private final String type;
    private final String subType;
    private final boolean negated;
    private final float quality;

    private MediaTypeExpression(String type, String subType, float quality, boolean negated) {
        this.type = type;
        this.subType = subType;
        this.quality = quality;
        this.negated = negated;
    }

    public MediaTypeExpression(String type, String subType) {
        this.type = type;
        this.subType = subType;
        quality = 1.0F;
        negated = false;
    }

    public static List<MediaType> toMediaTypes(List<MediaTypeExpression> expressions) {
        int size = expressions.size();
        List<MediaType> mediaTypes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            MediaTypeExpression expr = expressions.get(i);
            mediaTypes.add(new MediaType(expr.getType(), expr.getSubType()));
        }
        return mediaTypes;
    }

    public static MediaTypeExpression parse(String expr) {
        boolean negated;
        if (expr.indexOf('!') == 0) {
            negated = true;
            expr = expr.substring(1);
        } else {
            negated = false;
        }
        if (StringUtils.isEmpty(expr)) {
            return null;
        }

        int index = expr.indexOf(';');
        String mimeType = (index == -1 ? expr : expr.substring(0, index)).trim();
        if (MediaType.WILDCARD.equals(mimeType)) {
            mimeType = "*/*";
        }
        int subIndex = mimeType.indexOf('/');
        if (subIndex == -1 || subIndex == mimeType.length() - 1) {
            return null;
        }
        String type = mimeType.substring(0, subIndex);
        String subType = mimeType.substring(subIndex + 1);
        if (MediaType.WILDCARD.equals(type) && !MediaType.WILDCARD.equals(subType)) {
            return null;
        }

        return new MediaTypeExpression(type, subType, HttpUtils.parseQuality(expr, index), negated);
    }

    private static int compareType(String type1, String type2) {
        boolean type1IsWildcard = MediaType.WILDCARD.equals(type1);
        boolean type2IsWildcard = MediaType.WILDCARD.equals(type2);
        if (type1IsWildcard && !type2IsWildcard) {
            return 1;
        }
        if (type2IsWildcard && !type1IsWildcard) {
            return -1;
        }
        if (!type1.equals(type2)) {
            return 0;
        }
        return Integer.MIN_VALUE;
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }

    public float getQuality() {
        return quality;
    }

    private static int compareQuality(MediaTypeExpression m1, MediaTypeExpression m2) {
        return Float.compare(m2.quality, m1.quality);
    }

    public boolean typesEquals(MediaTypeExpression other) {
        return type.equalsIgnoreCase(other.type) && subType.equalsIgnoreCase(other.subType);
    }

    public boolean match(MediaTypeExpression other) {
        return matchMediaType(other) != negated;
    }

    private boolean matchMediaType(MediaTypeExpression other) {
        if (other == null) {
            return false;
        }
        if (isWildcardType()) {
            return true;
        }
        if (type.equals(other.type)) {
            if (subType.equals(other.subType)) {
                return true;
            }
            if (isWildcardSubtype()) {
                int plusIdx = subType.lastIndexOf('+');
                if (plusIdx == -1) {
                    return true;
                }
                int otherPlusIdx = other.subType.indexOf('+');
                if (otherPlusIdx != -1) {
                    String subTypeNoSuffix = subType.substring(0, plusIdx);
                    String subTypeSuffix = subType.substring(plusIdx + 1);
                    String otherSubtypeSuffix = other.subType.substring(otherPlusIdx + 1);
                    return subTypeSuffix.equals(otherSubtypeSuffix) && MediaType.WILDCARD.equals(subTypeNoSuffix);
                }
            }
        }
        return false;
    }

    public boolean compatibleWith(MediaTypeExpression other) {
        return compatibleWithMediaType(other) != negated;
    }

    private boolean compatibleWithMediaType(MediaTypeExpression other) {
        if (other == null) {
            return false;
        }
        if (isWildcardType() || other.isWildcardType()) {
            return true;
        }
        if (type.equals(other.type)) {
            if (subType.equalsIgnoreCase(other.subType)) {
                return true;
            }
            if (isWildcardSubtype() || other.isWildcardSubtype()) {
                if (subType.equals(MediaType.WILDCARD) || other.subType.equals(MediaType.WILDCARD)) {
                    return true;
                }
                String thisSuffix = getSubtypeSuffix();
                String otherSuffix = other.getSubtypeSuffix();
                if (isWildcardSubtype() && thisSuffix != null) {
                    return (thisSuffix.equals(other.subType) || thisSuffix.equals(otherSuffix));
                }
                if (other.isWildcardSubtype() && otherSuffix != null) {
                    return (subType.equals(otherSuffix) || otherSuffix.equals(thisSuffix));
                }
            }
        }
        return false;
    }

    private boolean isWildcardType() {
        return MediaType.WILDCARD.equals(type);
    }

    private boolean isWildcardSubtype() {
        return MediaType.WILDCARD.equals(subType) || subType.startsWith("*+");
    }

    private String getSubtypeSuffix() {
        int suffixIndex = subType.lastIndexOf('+');
        if (suffixIndex != -1) {
            return subType.substring(suffixIndex + 1);
        }
        return null;
    }

    @Override
    public int compareTo(MediaTypeExpression other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subType, negated, quality);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != MediaTypeExpression.class) {
            return false;
        }
        MediaTypeExpression other = (MediaTypeExpression) obj;
        return negated == other.negated
                && Float.compare(quality, other.quality) == 0
                && Objects.equals(type, other.type)
                && Objects.equals(subType, other.subType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (negated) {
            sb.append('!');
        }
        sb.append(type).append('/').append(subType);
        if (quality != 1.0F) {
            sb.append(";q=").append(quality);
        }
        return sb.toString();
    }
}
