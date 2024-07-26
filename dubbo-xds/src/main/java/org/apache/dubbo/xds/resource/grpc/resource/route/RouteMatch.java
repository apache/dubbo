package org.apache.dubbo.xds.resource.grpc.resource.route;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.xds.resource.grpc.resource.matcher.FractionMatcher;
import org.apache.dubbo.xds.resource.grpc.resource.matcher.HeaderMatcher;

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

    PathMatcher pathMatcher() {
        return pathMatcher;
    }

    List<HeaderMatcher> headerMatchers() {
        return headerMatchers;
    }

    @Nullable
    FractionMatcher fractionMatcher() {
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
            return this.pathMatcher.equals(that.pathMatcher()) && this.headerMatchers.equals(that.headerMatchers()) && (
                    this.fractionMatcher == null ?
                            that.fractionMatcher() == null : this.fractionMatcher.equals(that.fractionMatcher()));
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

}
