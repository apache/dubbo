package org.apache.dubbo.xds.security.authz.rule.matcher;

import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.xds.security.authz.rule.RequestAuthProperty;
import org.apache.dubbo.xds.security.authz.rule.matcher.StringMatcher.MatchType;

import java.util.HashMap;
import java.util.Map;

import io.envoyproxy.envoy.config.core.v3.CidrRange;
import io.envoyproxy.envoy.config.route.v3.HeaderMatcher;
import io.envoyproxy.envoy.type.matcher.v3.RegexMatcher;

public class Matchers {

    public static MapMatcher mapMatcher(Map<String,String> valueMap,RequestAuthProperty propertyType,StringMatcher.MatchType matchType){
        Map<String,Matcher<String>> matcherMap = new HashMap<>(valueMap.size());
        valueMap.forEach((k,v)-> matcherMap.put(k,stringMatcher(v,propertyType)));
        return new MapMatcher(matcherMap,propertyType);
    }

   public static IpMatcher ipMatcher(CidrRange range, RequestAuthProperty authProperty){
       return new IpMatcher(range.getPrefixLen().getValue(), range.getAddressPrefix(),authProperty);
   }

   public static KeyMatcher keyMatcher(String key,StringMatcher stringMatcher){
       return new KeyMatcher(key, stringMatcher);
   }

   public static StringMatcher stringMatcher(String value, RequestAuthProperty property){
       return new StringMatcher(MatchType.EXACT,value,property);
   }

    public static StringMatcher stringMatcher(io.envoyproxy.envoy.type.matcher.v3.StringMatcher stringMatcher, RequestAuthProperty authProperty){
        String exact = stringMatcher.getExact();
        String prefix = stringMatcher.getPrefix();
        String suffix = stringMatcher.getSuffix();
        String contains = stringMatcher.getContains();
        String regex = stringMatcher.getSafeRegex().getRegex();
        if (StringUtils.isNotBlank(exact)) {
            return new StringMatcher(MatchType.EXACT,exact,authProperty);
        }
        if (StringUtils.isNotBlank(prefix)) {
            return new StringMatcher(MatchType.PREFIX,prefix,authProperty);
        }
        if (StringUtils.isNotBlank(suffix)) {
            return new StringMatcher(MatchType.SUFFIX,suffix,authProperty);
        }
        if (StringUtils.isNotBlank(contains)) {
            return new StringMatcher(MatchType.CONTAIN,contains,authProperty);
        }
        if (StringUtils.isNotBlank(regex)) {
            return new StringMatcher(MatchType.REGEX,regex,authProperty);
        }
        return null;
    }

    public static StringMatcher stringMatcher(HeaderMatcher headerMatcher,RequestAuthProperty authProperty) {
        return stringMatcher(headerMatch2StringMatch(headerMatcher),authProperty);
    }

    public static io.envoyproxy.envoy.type.matcher.v3.StringMatcher headerMatch2StringMatch(
            HeaderMatcher headerMatcher) {
        if (headerMatcher == null) {
            return null;
        }
        if (headerMatcher.getPresentMatch()) {
            io.envoyproxy.envoy.type.matcher.v3.StringMatcher.Builder builder
                    = io.envoyproxy.envoy.type.matcher.v3.StringMatcher
                    .newBuilder();
            return builder.setSafeRegex(RegexMatcher.newBuilder().build())
                    .setIgnoreCase(true).build();
        }
        if (!headerMatcher.hasStringMatch()) {
            io.envoyproxy.envoy.type.matcher.v3.StringMatcher.Builder builder
                    = io.envoyproxy.envoy.type.matcher.v3.StringMatcher
                    .newBuilder();
            String exactMatch = headerMatcher.getExactMatch();
            String containsMatch = headerMatcher.getContainsMatch();
            String prefixMatch = headerMatcher.getPrefixMatch();
            String suffixMatch = headerMatcher.getSuffixMatch();
            RegexMatcher safeRegex = headerMatcher.getSafeRegexMatch();
            if (!StringUtils.isEmpty(exactMatch)) {
                builder.setExact(exactMatch);
            } else if (!StringUtils.isEmpty(containsMatch)) {
                builder.setContains(containsMatch);
            } else if (!StringUtils.isEmpty(prefixMatch)) {
                builder.setPrefix(prefixMatch);
            } else if (!StringUtils.isEmpty(suffixMatch)) {
                builder.setSuffix(suffixMatch);
            } else if (safeRegex.isInitialized()) {
                builder.setSafeRegex(safeRegex);
            }
            return builder.setIgnoreCase(true).build();
        }
        return headerMatcher.getStringMatch();
    }


    public static StringMatcher toStringMatcher(HeaderMatcher headerMatcher, RequestAuthProperty property) {
        return toStringMatcher(headerMatch2StringMatch(headerMatcher),property);
    }


    public static StringMatcher toStringMatcher(io.envoyproxy.envoy.type.matcher.v3.StringMatcher stringMatcher, RequestAuthProperty authProperty) {
        if (stringMatcher == null) {
            return null;
        }
        boolean ignoreCase = stringMatcher.getIgnoreCase();
        String exact = stringMatcher.getExact();
        String prefix = stringMatcher.getPrefix();
        String suffix = stringMatcher.getSuffix();
        String contains = stringMatcher.getContains();
        String regex = stringMatcher.getSafeRegex().getRegex();
        if (StringUtils.isNotBlank(exact)) {
            return new StringMatcher(MatchType.EXACT, prefix,authProperty);
        }
        if (StringUtils.isNotBlank(prefix)) {
            return new StringMatcher(MatchType.PREFIX, prefix,authProperty);
        }
        if (StringUtils.isNotBlank(suffix)) {
            return new StringMatcher(MatchType.SUFFIX, prefix,authProperty);
        }
        if (StringUtils.isNotBlank(contains)) {
            return new StringMatcher(MatchType.CONTAIN, prefix,authProperty);
        }
        if (StringUtils.isNotBlank(regex)) {
            return new StringMatcher(MatchType.REGEX, prefix,authProperty);
        }
        return null;
    }

}
