package org.apache.dubbo.xds.resource.grpc.resource.route;

import org.apache.dubbo.common.lang.Nullable;

import com.google.re2j.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public class PathMatcher {

    @Nullable
    private final String path;

    @Nullable
    private final String prefix;

    @Nullable
    private final Pattern regEx;

    private final boolean caseSensitive;

    public static PathMatcher fromPath(String path, boolean caseSensitive) {
        checkNotNull(path, "path");
        return create(path, null, null, caseSensitive);
    }

    public static PathMatcher fromPrefix(String prefix, boolean caseSensitive) {
        checkNotNull(prefix, "prefix");
        return create(null, prefix, null, caseSensitive);
    }

    public static PathMatcher fromRegEx(Pattern regEx) {
        checkNotNull(regEx, "regEx");
        return create(null, null, regEx, false /* doesn't matter */);
    }

    private static PathMatcher create(@Nullable String path, @Nullable String prefix,
                                                                   @Nullable Pattern regEx, boolean caseSensitive) {
        return new PathMatcher(path, prefix, regEx,
                caseSensitive);
    }

    PathMatcher(
            @Nullable String path, @Nullable String prefix, @Nullable Pattern regEx, boolean caseSensitive) {
        this.path = path;
        this.prefix = prefix;
        this.regEx = regEx;
        this.caseSensitive = caseSensitive;
    }

    @Nullable
    String path() {
        return path;
    }

    @Nullable
    String prefix() {
        return prefix;
    }

    @Nullable
    Pattern regEx() {
        return regEx;
    }

    boolean caseSensitive() {
        return caseSensitive;
    }

    public String toString() {
        return "PathMatcher{" + "path=" + path + ", " + "prefix=" + prefix + ", " + "regEx=" + regEx + ", "
                + "caseSensitive=" + caseSensitive + "}";
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof PathMatcher) {
            PathMatcher that = (PathMatcher) o;
            return (this.path == null ? that.path() == null : this.path.equals(that.path())) && (
                    this.prefix == null ? that.prefix() == null : this.prefix.equals(that.prefix())) && (
                    this.regEx == null ? that.regEx() == null : this.regEx.equals(that.regEx()))
                    && this.caseSensitive == that.caseSensitive();
        }
        return false;
    }

    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= (path == null) ? 0 : path.hashCode();
        h$ *= 1000003;
        h$ ^= (prefix == null) ? 0 : prefix.hashCode();
        h$ *= 1000003;
        h$ ^= (regEx == null) ? 0 : regEx.hashCode();
        h$ *= 1000003;
        h$ ^= caseSensitive ? 1231 : 1237;
        return h$;
    }

}
