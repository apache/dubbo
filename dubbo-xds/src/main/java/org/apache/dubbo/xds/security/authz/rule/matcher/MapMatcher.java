package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import java.util.Map;

/**
 * supports multiple keys and values
 */
public class MapMatcher implements Matcher<Map<String,String>>{

    private Map<String,Matcher<String>> keyToMatchers;

    private RequestAuthProperty property;

    public MapMatcher(Map<String,Matcher<String>> matcherMap,RequestAuthProperty property){
        this.keyToMatchers = matcherMap;
    }

    @Override
    public boolean match(Map<String, String> actualValues) {
        for (String key : keyToMatchers.keySet()) {
            Matcher<String> matcher = keyToMatchers.get(key);
            String actual = actualValues.get(key);
            if (!matcher.match(actual)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public RequestAuthProperty propType() {
        return property;
    }
}
