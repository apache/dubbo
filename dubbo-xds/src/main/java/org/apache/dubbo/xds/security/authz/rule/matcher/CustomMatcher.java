package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import java.util.function.Function;

public class CustomMatcher<T> implements Matcher<T>{

    private RequestAuthProperty property;

    private Function<T,Boolean> matchFunction;

    public CustomMatcher(RequestAuthProperty property,Function<T,Boolean> matchFunction){
        this.matchFunction = matchFunction;
        this.property = property;
    }

    @Override
    public boolean match(T actual) {
        return matchFunction.apply(actual);
    }

    @Override
    public RequestAuthProperty propType() {
        return property;
    }
}
