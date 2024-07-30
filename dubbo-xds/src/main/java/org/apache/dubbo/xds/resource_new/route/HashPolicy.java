package org.apache.dubbo.xds.resource_new.route;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.Assert;

import com.google.re2j.Pattern;

public class HashPolicy {

    private final HashPolicyType type;

    private final boolean isTerminal;

    @Nullable
    private final String headerName;

    @Nullable
    private final Pattern regEx;

    @Nullable
    private final String regExSubstitution;

    public static HashPolicy forHeader(
            boolean isTerminal, String headerName, @Nullable Pattern regEx, @Nullable String regExSubstitution) {
        Assert.notNull(headerName, "headerName must not be null");
        return HashPolicy.create(HashPolicyType.HEADER, isTerminal, headerName, regEx, regExSubstitution);
    }

    public static HashPolicy forChannelId(boolean isTerminal) {
        return HashPolicy.create(HashPolicyType.CHANNEL_ID, isTerminal, null, null, null);
    }

    public static HashPolicy create(
            HashPolicyType type,
            boolean isTerminal,
            @Nullable String headerName,
            @Nullable Pattern regEx,
            @Nullable String regExSubstitution) {
        return new HashPolicy(type, isTerminal, headerName, regEx, regExSubstitution);
    }

    HashPolicy(
            HashPolicyType type,
            boolean isTerminal,
            @Nullable String headerName,
            @Nullable Pattern regEx,
            @Nullable String regExSubstitution) {
        if (type == null) {
            throw new NullPointerException("Null type");
        }
        this.type = type;
        this.isTerminal = isTerminal;
        this.headerName = headerName;
        this.regEx = regEx;
        this.regExSubstitution = regExSubstitution;
    }

    HashPolicyType type() {
        return type;
    }

    boolean isTerminal() {
        return isTerminal;
    }

    @Nullable
    String headerName() {
        return headerName;
    }

    @Nullable
    Pattern regEx() {
        return regEx;
    }

    @Nullable
    String regExSubstitution() {
        return regExSubstitution;
    }

    @Override
    public String toString() {
        return "HashPolicy{" + "type=" + type + ", " + "isTerminal=" + isTerminal + ", " + "headerName=" + headerName
                + ", " + "regEx=" + regEx + ", " + "regExSubstitution=" + regExSubstitution + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof HashPolicy) {
            HashPolicy that = (HashPolicy) o;
            return this.type.equals(that.type()) && this.isTerminal == that.isTerminal() && (
                    this.headerName == null ? that.headerName() == null : this.headerName.equals(that.headerName()))
                    && (this.regEx == null ? that.regEx() == null : this.regEx.equals(that.regEx())) && (
                    this.regExSubstitution == null ?
                            that.regExSubstitution() == null : this.regExSubstitution.equals(that.regExSubstitution()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h$ = 1;
        h$ *= 1000003;
        h$ ^= type.hashCode();
        h$ *= 1000003;
        h$ ^= isTerminal ? 1231 : 1237;
        h$ *= 1000003;
        h$ ^= (headerName == null) ? 0 : headerName.hashCode();
        h$ *= 1000003;
        h$ ^= (regEx == null) ? 0 : regEx.hashCode();
        h$ *= 1000003;
        h$ ^= (regExSubstitution == null) ? 0 : regExSubstitution.hashCode();
        return h$;
    }

}
