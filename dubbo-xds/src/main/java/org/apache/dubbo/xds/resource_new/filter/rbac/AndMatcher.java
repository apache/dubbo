package org.apache.dubbo.xds.resource_new.filter.rbac;

import org.apache.dubbo.common.utils.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class AndMatcher implements Matcher {

    private final List<? extends Matcher> allMatch;

    AndMatcher(
            List<? extends Matcher> allMatch) {
        if (allMatch == null) {
            throw new NullPointerException("Null allMatch");
        }
        this.allMatch = allMatch;
    }

    /**
     * Matches when all of the matchers match.
     */
    public static AndMatcher create(List<? extends Matcher> matchers) {
        Assert.notNull(matchers, "matchers must not be null");
        for (Matcher matcher : matchers) {
            Assert.notNull(matcher, "matcher must not be null");
        }
        return new AndMatcher(Collections.unmodifiableList(new ArrayList<>(matchers)));
    }

    public static AndMatcher create(Matcher... matchers) {
        return AndMatcher.create(Arrays.asList(matchers));
    }

    @Override
    public boolean matches(Object args) {
        for (Matcher m : allMatch()) {
            if (!m.matches(args)) {
                return false;
            }
        }
        return true;
    }

    public List<? extends Matcher> allMatch() {
        return allMatch;
    }

    @Override
    public String toString() {
        return "AndMatcher{" + "allMatch=" + allMatch + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof AndMatcher) {
            AndMatcher that = (AndMatcher) o;
            return this.allMatch.equals(that.allMatch());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= allMatch.hashCode();
        return h$;
    }

}
