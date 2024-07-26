/*
 * Copyright 2021 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc.resource.matcher;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;

// TODO(zivy@): may reuse common matchers parsers.
public final class MatcherParser {
  /** Translates envoy proto HeaderMatcher to internal HeaderMatcher.*/
  public static HeaderMatcher parseHeaderMatcher(
          io.envoyproxy.envoy.config.route.v3.HeaderMatcher proto) {
    switch (proto.getHeaderMatchSpecifierCase()) {
      case EXACT_MATCH:
        return HeaderMatcher.forExactValue(
                        proto.getName(), proto.getExactMatch(), proto.getInvertMatch());
      case SAFE_REGEX_MATCH:
        String rawPattern = proto.getSafeRegexMatch().getRegex();
        Pattern safeRegExMatch;
        try {
          safeRegExMatch = Pattern.compile(rawPattern);
        } catch (PatternSyntaxException e) {
          throw new IllegalArgumentException(
                "HeaderMatcher [" + proto.getName() + "] contains malformed safe regex pattern: "
                        + e.getMessage());
        }
        return HeaderMatcher.forSafeRegEx(
              proto.getName(), safeRegExMatch, proto.getInvertMatch());
      case RANGE_MATCH:
        Range rangeMatch = new Range(
              proto.getRangeMatch().getStart(), proto.getRangeMatch().getEnd());
        return HeaderMatcher.forRange(
              proto.getName(), rangeMatch, proto.getInvertMatch());
      case PRESENT_MATCH:
        return HeaderMatcher.forPresent(
              proto.getName(), proto.getPresentMatch(), proto.getInvertMatch());
      case PREFIX_MATCH:
        return HeaderMatcher.forPrefix(
              proto.getName(), proto.getPrefixMatch(), proto.getInvertMatch());
      case SUFFIX_MATCH:
        return HeaderMatcher.forSuffix(
              proto.getName(), proto.getSuffixMatch(), proto.getInvertMatch());
      case CONTAINS_MATCH:
        return HeaderMatcher.forContains(
              proto.getName(), proto.getContainsMatch(), proto.getInvertMatch());
      case STRING_MATCH:
        return HeaderMatcher.forString(
          proto.getName(), parseStringMatcher(proto.getStringMatch()), proto.getInvertMatch());
      case HEADERMATCHSPECIFIER_NOT_SET:
      default:
        throw new IllegalArgumentException(
                "Unknown header matcher type: " + proto.getHeaderMatchSpecifierCase());
    }
  }

  /** Translate StringMatcher envoy proto to internal StringMatcher. */
  public static StringMatcher parseStringMatcher(
            io.envoyproxy.envoy.type.matcher.v3.StringMatcher proto) {
    switch (proto.getMatchPatternCase()) {
      case EXACT:
        return StringMatcher.forExact(proto.getExact(), proto.getIgnoreCase());
      case PREFIX:
        return StringMatcher.forPrefix(proto.getPrefix(), proto.getIgnoreCase());
      case SUFFIX:
        return StringMatcher.forSuffix(proto.getSuffix(), proto.getIgnoreCase());
      case SAFE_REGEX:
        return StringMatcher.forSafeRegEx(
                Pattern.compile(proto.getSafeRegex().getRegex()));
      case CONTAINS:
        return StringMatcher.forContains(proto.getContains());
      case MATCHPATTERN_NOT_SET:
      default:
        throw new IllegalArgumentException(
                "Unknown StringMatcher match pattern: " + proto.getMatchPatternCase());
    }
  }
}
