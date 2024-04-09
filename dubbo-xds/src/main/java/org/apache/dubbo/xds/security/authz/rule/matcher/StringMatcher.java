package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;

import java.util.regex.Pattern;

import static org.apache.dubbo.xds.security.authz.rule.matcher.StringMatcher.MatcherType.EXACT;

public class StringMatcher implements Matcher<String>{

    private String condition;

    private MatcherType matcherType;

    private RequestAuthProperty authProperty;

    private ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(StringMatcher.class);

    private boolean not = false;

    public static StringMatcher ofType(io.envoyproxy.envoy.type.matcher.v3.StringMatcher stringMatcher, RequestAuthProperty authProperty){
        String exact = stringMatcher.getExact();
        String prefix = stringMatcher.getPrefix();
        String suffix = stringMatcher.getSuffix();
        String contains = stringMatcher.getContains();
        String regex = stringMatcher.getSafeRegex().getRegex();
        if (StringUtils.isNotBlank(exact)) {
            return new StringMatcher(EXACT,exact,authProperty);
        }
        if (StringUtils.isNotBlank(prefix)) {
            return new StringMatcher(StringMatcher.MatcherType.PREFIX,prefix,authProperty);
        }
        if (StringUtils.isNotBlank(suffix)) {
            return new StringMatcher(StringMatcher.MatcherType.SUFFIX,suffix,authProperty);
        }
        if (StringUtils.isNotBlank(contains)) {
            return new StringMatcher(StringMatcher.MatcherType.CONTAIN,contains,authProperty);
        }
        if (StringUtils.isNotBlank(regex)) {
            return new StringMatcher(StringMatcher.MatcherType.REGEX,regex,authProperty);
        }
        return null;
    }

    public StringMatcher(MatcherType matcherType,String condition, RequestAuthProperty authProperty) {
        this.matcherType = matcherType;
        this.condition = condition;
        this.authProperty = authProperty;
    }

    public StringMatcher(MatcherType matcherType,String condition, RequestAuthProperty authProperty, boolean not) {
        this.matcherType = matcherType;
        this.condition = condition;
        this.authProperty = authProperty;
        this.not = not;
    }


    public boolean match(String actual) {
        boolean res;
        if (StringUtils.isEmpty(actual)) {
            res = false;
        }else {
            switch (matcherType) {
                case EXACT:
                    res = actual.equals(condition);
                    break;
                case PREFIX:
                    res = actual.startsWith(condition);
                    break;
                case SUFFIX:
                    res = actual.endsWith(condition);
                    break;
                case CONTAIN:
                    res = actual.contains(condition);
                    break;
                case REGEX:
                    try {
                        res = Pattern.matches(condition, actual);
                        break;
                    } catch (Exception e) {
                        logger.warn("", "", "", "Irregular matching,key={},str={}", e);
                        return false;
                    }
                default:
                    throw new UnsupportedOperationException("unsupported string compare operation");
            }
        }
        return not ^ res;

    }

    @Override
    public RequestAuthProperty propType() {
        return authProperty;
    }

    public enum MatcherType {

        /**
         * exact match.
         */
        EXACT("exact"),
        /**
         * prefix match.
         */
        PREFIX("prefix"),
        /**
         * suffix match.
         */
        SUFFIX("suffix"),
        /**
         * regex match.
         */
        REGEX("regex"),
        /**
         * contain match.
         */
        CONTAIN("contain");

        /**
         * type of matcher.
         */
        public final String key;

        MatcherType(String type) {
            this.key = type;
        }

        @Override
        public String toString() {
            return this.key;
        }

    }

}
