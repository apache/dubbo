package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;
import org.apache.dubbo.xds.security.authz.rule.matcher.StringMatcher.MatchType;

import java.util.Map;

public class KeyMatcher implements Matcher<Map<String, String>>{

    private String key;

    private StringMatcher stringMatcher;

    public KeyMatcher(MatchType matchType, String condition, RequestAuthProperty authProperty, String key) {
        this.stringMatcher = new StringMatcher(matchType, condition, authProperty);
        this.key = key;
    }

    public KeyMatcher(String key,StringMatcher stringMatcher){
        this.key = key;
        this.stringMatcher = stringMatcher;
    }

    @Override
    public boolean match(Map<String,String> actual) {
        String toMatch = actual.get(key);
        if(toMatch == null){
            return false;
        }
        return this.stringMatcher.match(toMatch);
    }


    @Override
    public RequestAuthProperty propType() {
        return this.stringMatcher.propType();
    }
}
