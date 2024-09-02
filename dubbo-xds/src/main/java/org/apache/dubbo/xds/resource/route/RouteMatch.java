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
package org.apache.dubbo.xds.resource.route;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.matcher.FractionMatcher;
import org.apache.dubbo.xds.resource.matcher.HeaderMatcher;
import org.apache.dubbo.xds.resource.matcher.PathMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RouteMatch {

    private final PathMatcher pathMatcher;

    private final List<HeaderMatcher> headerMatchers;

    @Nullable
    private final FractionMatcher fractionMatcher;

    public RouteMatch(
            PathMatcher pathMatcher, List<HeaderMatcher> headerMatchers, @Nullable FractionMatcher fractionMatcher) {
        if (pathMatcher == null) {
            throw new NullPointerException("Null pathMatcher");
        }
        this.pathMatcher = pathMatcher;
        if (headerMatchers == null) {
            throw new NullPointerException("Null headerMatchers");
        }
        this.headerMatchers = Collections.unmodifiableList(new ArrayList<>(headerMatchers));
        this.fractionMatcher = fractionMatcher;
    }

    public PathMatcher getPathMatcher() {
        return pathMatcher;
    }

    public List<HeaderMatcher> getHeaderMatchers() {
        return headerMatchers;
    }

    @Nullable
    public FractionMatcher getFractionMatcher() {
        return fractionMatcher;
    }

    public String toString() {
        return "RouteMatch{" + "pathMatcher=" + pathMatcher + ", " + "headerMatchers=" + headerMatchers + ", "
                + "fractionMatcher=" + fractionMatcher + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof RouteMatch) {
            RouteMatch that = (RouteMatch) o;
            return this.pathMatcher.equals(that.getPathMatcher())
                    && this.headerMatchers.equals(that.getHeaderMatchers())
                    && (this.fractionMatcher == null
                            ? that.getFractionMatcher() == null
                            : this.fractionMatcher.equals(that.getFractionMatcher()));
        }
        return false;
    }

    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= pathMatcher.hashCode();
        h$ *= 1000003;
        h$ ^= headerMatchers.hashCode();
        h$ *= 1000003;
        h$ ^= (fractionMatcher == null) ? 0 : fractionMatcher.hashCode();
        return h$;
    }

    public boolean isPathMatch(String input) {
        return pathMatcher.isMatch(input);
    }
}
