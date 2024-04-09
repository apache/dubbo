package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

/**
 * @param <T> Type of the actual value to match.
 */
public interface Matcher<T> {
    boolean match(T actual);

    RequestAuthProperty propType();
}
