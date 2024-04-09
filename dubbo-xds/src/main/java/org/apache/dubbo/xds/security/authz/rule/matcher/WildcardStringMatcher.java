package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

/**
 * Supports simple '*' match
 */
public class WildcardStringMatcher implements Matcher<String>{

    private String value;

    private RequestAuthProperty authProperty;


    public WildcardStringMatcher(String value, RequestAuthProperty authProperty) {
        this.value =  parseToPattern(value);
        this.authProperty = authProperty;
    }

    @Override
    public boolean match(String actual) {
        String pattern = parseToPattern(value);
        return actual.matches(pattern);
    }

    private String parseToPattern(String val) {
        StringBuilder patternBuilder = new StringBuilder();
        for (int i = 0; i < val.length(); i++) {
            char c = val.charAt(i);
            switch (c) {
                case '*':
                    patternBuilder.append(".*");
                    break;
                case '\\':
                case '.':
                case '^':
                case '$':
                case '+':
                case '?':
                case '{':
                case '}':
                case '[':
                case ']':
                case '|':
                case '(':
                case ')':
                    patternBuilder.append("\\").append(c);
                    break;
                default:
                    patternBuilder.append(c);
                    break;
            }
        }
        return patternBuilder.toString();
    }


    @Override
    public RequestAuthProperty propType() {
        return authProperty;
    }
}
