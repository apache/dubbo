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
package org.apache.dubbo.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class is based on Dropwizard metrics, see io/dropwizard/metrics/MetricName.java
 *
 * The following changes are made:
 *   * Add metric level
 *   * Cache the hash code
 *
 */
public class MetricName implements Comparable<MetricName> {

    public static final String SEPARATOR = ".";
    public static final Map<String, String> EMPTY_TAGS = Collections.emptyMap();
    public static final MetricName EMPTY = new MetricName();

    private final String key;
    private final Map<String, String> tags;
    // the level to indicate the importance of a metric
    private MetricLevel level;

    private int hashCode = 0;
    
    private boolean hashCodeCached = false;
    
    public MetricName() {
        this(null, null, null);
    }

    public MetricName(String key) {
        this(key, null, null);
    }

    public MetricName(String key, Map<String, String> tags) {
        this(key, tags, null);
    }

    public MetricName(String key, MetricLevel level) {
        this(key, null, level);
    }

    public MetricName(String key, Map<String, String> tags, MetricLevel level) {
        this.key = key;
        this.tags = checkTags(tags);
        this.level = level == null ? MetricLevel.NORMAL : level;
    }

    private Map<String, String> checkTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return EMPTY_TAGS;
        }

        return Collections.unmodifiableMap(tags);
    }

    public String getKey() {
        return key;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    /**
     * Return the level of this metric
     * The level indicates the importance of the metric
     *
     * @return when level tag do not exist or illegal tag, will return null.
     */
    public MetricLevel getMetricLevel() {
        return level;
    }


    /**
     * Metric level can be changed during runtime
     * @param level the level to set
     */
    public MetricName level(MetricLevel level) {
        this.level = level;
        return this;
    }


    /**
     * @see {@link #resolve(String, boolean)}
     */
    public MetricName resolve(String p) {
        return resolve(p, true);
    }

    /**
     * Build the MetricName that is this with another path appended to it.
     *
     * The new MetricName inherits the tags of this one.
     *
     * @param p The extra path element to add to the new metric.
     * @param inheritTags if true, tags will be inherited
     * @return A new metric name relative to the original by the path specified
     *         in p.
     */
    public MetricName resolve(String p, boolean inheritTags) {
        final String next;

        if (p != null && !p.isEmpty()) {
            if (key != null && !key.isEmpty()) {
                next = key + SEPARATOR + p;
            } else {
                next = p;
            }
        } else {
            next = this.key;
        }

        return inheritTags ? new MetricName(next, tags, level) : new MetricName(next, level);
    }

    /**
     * Add tags to a metric name and return the newly created MetricName.
     *
     * @param add Tags to add.
     * @return A newly created metric name with the specified tags associated with it.
     */
    public MetricName tag(Map<String, String> add) {
        final Map<String, String> tags = new HashMap<String, String>(add);
        tags.putAll(this.tags);
        return new MetricName(key, tags, level);
    }

    /**
     * Same as {@link #tag(Map)}, but takes a variadic list
     * of arguments.
     *
     * @see #tag(Map)
     * @param pairs An even list of strings acting as key-value pairs.
     * @return A newly created metric name with the specified tags associated
     *         with it.
     */
    public MetricName tag(String... pairs) {
        if (pairs == null) {
            return this;
        }

        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Argument count must be even");
        }

        final Map<String, String> add = new HashMap<String, String>();

        for (int i = 0; i < pairs.length; i += 2) {
            add.put(pairs[i], pairs[i+1]);
        }

        return tag(add);
    }

    /**
     * Join the specified set of metric names.
     *
     * @param parts Multiple metric names to join using the separator.
     * @return A newly created metric name which has the name of the specified
     *         parts and includes all tags of all child metric names.
     **/
    public static MetricName join(MetricName... parts) {
        final StringBuilder nameBuilder = new StringBuilder();
        final Map<String, String> tags = new HashMap<String, String>();

        boolean first = true;
        MetricName firstName = null;

        for (MetricName part : parts) {
            final String name = part.getKey();

            if (name != null && !name.isEmpty()) {
                if (first) {
                    first = false;
                    firstName = part;
                } else {
                    nameBuilder.append(SEPARATOR);
                }

                nameBuilder.append(name);
            }

            if (!part.getTags().isEmpty()) {
                tags.putAll(part.getTags());
            }
        }

        MetricLevel level = firstName == null ? null : firstName.getMetricLevel();
        return new MetricName(nameBuilder.toString(), tags, level);
    }

    /**
     * Build a new metric name using the specific path components.
     *
     * @param parts Path of the new metric name.
     * @return A newly created metric name with the specified path.
     **/
    public static MetricName build(String... parts) {
        if (parts == null || parts.length == 0) {
            return MetricName.EMPTY;
        }

        if (parts.length == 1) {
            return new MetricName(parts[0], EMPTY_TAGS);
        }

        return new MetricName(buildName(parts), EMPTY_TAGS);
    }

    private static String buildName(String... names) {
        final StringBuilder builder = new StringBuilder();
        boolean first = true;

        for (String name : names) {
            if (name == null || name.isEmpty()) {
                continue;
            }

            if (first) {
                first = false;
            } else {
                builder.append(SEPARATOR);
            }

            builder.append(name);
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        if (tags.isEmpty()) {
            return key;
        }

        return key + tags;
    }

    @Override
    public int hashCode() {
        
        if (!hashCodeCached){
            
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((tags == null) ? 0 : tags.hashCode());
            
            hashCode = result;
            hashCodeCached = true;
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        MetricName other = (MetricName) obj;

        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }

        if (!tags.equals(other.tags)) {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(MetricName o) {
        if (o == null) {
            return -1;
        }

        int c = compareName(key, o.getKey());

        if (c != 0) {
            return c;
        }

        return compareTags(tags, o.getTags());
    }

    private int compareName(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }

        if (left == null) {
            return 1;
        }

        if (right == null) {
            return -1;
        }

        return left.compareTo(right);
    }

    private int compareTags(Map<String, String> left, Map<String, String> right) {
        if (left == null && right == null) {
            return 0;
        }

        if (left == null) {
            return 1;
        }

        if (right == null) {
            return -1;
        }

        final Iterable<String> keys = uniqueSortedKeys(left, right);

        for (final String key : keys) {
            final String a = left.get(key);
            final String b = right.get(key);

            if (a == null && b == null) {
                continue;
            }

            if (a == null) {
                return -1;
            }

            if (b == null) {
                return 1;
            }

            int c = a.compareTo(b);

            if (c != 0) {
                return c;
            }
        }

        return 0;
    }

    private Iterable<String> uniqueSortedKeys(Map<String, String> left, Map<String, String> right) {
        final Set<String> set = new TreeSet<String>(left.keySet());
        set.addAll(right.keySet());
        return set;
    }
}
