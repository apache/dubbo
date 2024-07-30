package org.apache.dubbo.xds.resource_new.matcher;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.Assert;

import com.google.re2j.Pattern;

import org.apache.dubbo.xds.resource_new.common.Range;

public final class HeaderMatcher {

    private final String name;

    @Nullable
    private final String exactValue;

    @Nullable
    private final Pattern safeRegEx;

    @Nullable
    private final Range range;

    @Nullable
    private final Boolean present;

    @Nullable
    private final String prefix;

    @Nullable
    private final String suffix;

    @Nullable
    private final String contains;

    @Nullable
    private final StringMatcher stringMatcher;

    private final boolean inverted;

    /**
     * The request header value should exactly match the specified value.
     */
    public static HeaderMatcher forExactValue(String name, String exactValue, boolean inverted) {
        Assert.notNull(name, "name must not be null");
        Assert.notNull(exactValue, "exactValue must not be null");
        return HeaderMatcher.create(name, exactValue, null, null, null, null, null, null, null, inverted);
    }

    /**
     * The request header value should match the regular expression pattern.
     */
    public static HeaderMatcher forSafeRegEx(String name, Pattern safeRegEx, boolean inverted) {
        Assert.notNull(name, "name must not be null");
        Assert.notNull(safeRegEx, "safeRegEx must not be null");
        return HeaderMatcher.create(name, null, safeRegEx, null, null, null, null, null, null, inverted);
    }

    /**
     * The request header value should be within the range.
     */
    public static HeaderMatcher forRange(String name, Range range, boolean inverted) {
        Assert.notNull(name, "name must not be null");
        Assert.notNull(range, "range must not be null");
        return HeaderMatcher.create(name, null, null, range, null, null, null, null, null, inverted);
    }

    /**
     * The request header value should exist.
     */
    public static HeaderMatcher forPresent(String name, boolean present, boolean inverted) {
        Assert.notNull(name, "name must not be null");
        return HeaderMatcher.create(name, null, null, null, present, null, null, null, null, inverted);
    }

    /**
     * The request header value should have this prefix.
     */
    public static HeaderMatcher forPrefix(String name, String prefix, boolean inverted) {
        Assert.notNull(name, "name must not be null");
        Assert.notNull(prefix, "prefix must not be null");
        return HeaderMatcher.create(name, null, null, null, null, prefix, null, null, null, inverted);
    }

    /**
     * The request header value should have this suffix.
     */
    public static HeaderMatcher forSuffix(String name, String suffix, boolean inverted) {
        Assert.notNull(name, "name must not be null");
        Assert.notNull(suffix, "suffix must not be null");
        return HeaderMatcher.create(name, null, null, null, null, null, suffix, null, null, inverted);
    }

    /**
     * The request header value should have this substring.
     */
    public static HeaderMatcher forContains(String name, String contains, boolean inverted) {
        Assert.notNull(name, "name must not be null");
        Assert.notNull(contains, "contains must not be null");
        return HeaderMatcher.create(name, null, null, null, null, null, null, contains, null, inverted);
    }

    /**
     * The request header value should match this stringMatcher.
     */
    public static HeaderMatcher forString(
            String name, StringMatcher stringMatcher, boolean inverted) {
        Assert.notNull(name, "name must not be null");
        Assert.notNull(stringMatcher, "stringMatcher must not be null");
        return HeaderMatcher.create(name, null, null, null, null, null, null, null, stringMatcher, inverted);
    }

    private static HeaderMatcher create(
            String name,
            @Nullable String exactValue,
            @Nullable Pattern safeRegEx,
            @Nullable Range range,
            @Nullable Boolean present,
            @Nullable String prefix,
            @Nullable String suffix,
            @Nullable String contains,
            @Nullable StringMatcher stringMatcher,
            boolean inverted) {
        Assert.notNull(name, "name");
        return new HeaderMatcher(name, exactValue, safeRegEx, range, present, prefix, suffix, contains, stringMatcher
                , inverted);
    }

    /**
     * Returns the matching result.
     */
    public boolean matches(@Nullable String value) {
        if (value == null) {
            return present() != null && present() == inverted();
        }
        boolean baseMatch;
        if (exactValue() != null) {
            baseMatch = exactValue().equals(value);
        } else if (safeRegEx() != null) {
            baseMatch = safeRegEx().matches(value);
        } else if (range() != null) {
            long numValue;
            try {
                numValue = Long.parseLong(value);
                baseMatch = numValue >= range().start() && numValue <= range().end();
            } catch (NumberFormatException ignored) {
                baseMatch = false;
            }
        } else if (prefix() != null) {
            baseMatch = value.startsWith(prefix());
        } else if (present() != null) {
            baseMatch = present();
        } else if (suffix() != null) {
            baseMatch = value.endsWith(suffix());
        } else if (contains() != null) {
            baseMatch = value.contains(contains());
        } else {
            baseMatch = stringMatcher().matches(value);
        }
        return baseMatch != inverted();
    }

    HeaderMatcher(
            String name,
            @Nullable String exactValue,
            @Nullable Pattern safeRegEx,
            @Nullable Range range,
            @Nullable Boolean present,
            @Nullable String prefix,
            @Nullable String suffix,
            @Nullable String contains,
            @Nullable StringMatcher stringMatcher,
            boolean inverted) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        this.name = name;
        this.exactValue = exactValue;
        this.safeRegEx = safeRegEx;
        this.range = range;
        this.present = present;
        this.prefix = prefix;
        this.suffix = suffix;
        this.contains = contains;
        this.stringMatcher = stringMatcher;
        this.inverted = inverted;
    }

    public String name() {
        return name;
    }

    @Nullable
    public String exactValue() {
        return exactValue;
    }

    @Nullable
    public Pattern safeRegEx() {
        return safeRegEx;
    }

    @Nullable
    public Range range() {
        return range;
    }

    @Nullable
    public Boolean present() {
        return present;
    }

    @Nullable
    public String prefix() {
        return prefix;
    }

    @Nullable
    public String suffix() {
        return suffix;
    }

    @Nullable
    public String contains() {
        return contains;
    }

    @Nullable
    public StringMatcher stringMatcher() {
        return stringMatcher;
    }

    public boolean inverted() {
        return inverted;
    }

    @Override
    public String toString() {
        return "HeaderMatcher{" + "name=" + name + ", " + "exactValue=" + exactValue + ", " + "safeRegEx=" + safeRegEx
                + ", " + "range=" + range + ", " + "present=" + present + ", " + "prefix=" + prefix + ", " + "suffix="
                + suffix + ", " + "contains=" + contains + ", " + "stringMatcher=" + stringMatcher + ", " + "inverted="
                + inverted + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof HeaderMatcher) {
            HeaderMatcher that = (HeaderMatcher) o;
            return this.name.equals(that.name()) && (
                    this.exactValue == null ? that.exactValue() == null : this.exactValue.equals(that.exactValue()))
                    && (this.safeRegEx == null ? that.safeRegEx() == null : this.safeRegEx.equals(that.safeRegEx()))
                    && (this.range == null ? that.range() == null : this.range.equals(that.range())) && (
                    this.present == null ? that.present() == null : this.present.equals(that.present())) && (
                    this.prefix == null ? that.prefix() == null : this.prefix.equals(that.prefix())) && (
                    this.suffix == null ? that.suffix() == null : this.suffix.equals(that.suffix())) && (
                    this.contains == null ? that.contains() == null : this.contains.equals(that.contains())) && (
                    this.stringMatcher == null ?
                            that.stringMatcher() == null : this.stringMatcher.equals(that.stringMatcher()))
                    && this.inverted == that.inverted();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= name.hashCode();
        h$ *= 1000003;
        h$ ^= (exactValue == null) ? 0 : exactValue.hashCode();
        h$ *= 1000003;
        h$ ^= (safeRegEx == null) ? 0 : safeRegEx.hashCode();
        h$ *= 1000003;
        h$ ^= (range == null) ? 0 : range.hashCode();
        h$ *= 1000003;
        h$ ^= (present == null) ? 0 : present.hashCode();
        h$ *= 1000003;
        h$ ^= (prefix == null) ? 0 : prefix.hashCode();
        h$ *= 1000003;
        h$ ^= (suffix == null) ? 0 : suffix.hashCode();
        h$ *= 1000003;
        h$ ^= (contains == null) ? 0 : contains.hashCode();
        h$ *= 1000003;
        h$ ^= (stringMatcher == null) ? 0 : stringMatcher.hashCode();
        h$ *= 1000003;
        h$ ^= inverted ? 1231 : 1237;
        return h$;
    }

}
