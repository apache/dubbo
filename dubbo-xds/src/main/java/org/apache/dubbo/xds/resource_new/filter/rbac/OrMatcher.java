package org.apache.dubbo.xds.resource_new.filter.rbac;

import org.apache.dubbo.common.utils.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class OrMatcher implements Matcher {

    private final List<? extends Matcher> anyMatch;

    /**
     * Matches when any of the matcher matches.
     */
    public static OrMatcher create(List<? extends Matcher> matchers) {
        Assert.notNull(matchers, "matchers must not be null");
        for (Matcher matcher : matchers) {
            Assert.notNull(matcher, "matcher must not be null");
        }
        return new OrMatcher(matchers);
    }

    public static OrMatcher create(Matcher... matchers) {
        return OrMatcher.create(Arrays.asList(matchers));
    }

    @Override
    public boolean matches(Object args) {
        for (Matcher m : anyMatch()) {
            if (m.matches(args)) {
                return true;
            }
        }
        return false;
    }

    OrMatcher(List<? extends Matcher> anyMatch) {
        if (anyMatch == null) {
            throw new NullPointerException("Null anyMatch");
        }
        this.anyMatch = Collections.unmodifiableList(new ArrayList<>(anyMatch));
    }

    public List<? extends Matcher> anyMatch() {
        return anyMatch;
    }

    @Override
    public String toString() {
        return "OrMatcher{" + "anyMatch=" + anyMatch + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof OrMatcher) {
            OrMatcher that = (OrMatcher) o;
            return this.anyMatch.equals(that.anyMatch());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= anyMatch.hashCode();
        return h$;
    }

}
