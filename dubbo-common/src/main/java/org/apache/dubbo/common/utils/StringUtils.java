/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.io.UnsafeStringWriter;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import com.alibaba.fastjson.JSON;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DOT_REGEX;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SEPARATOR_REGEX;
import static org.apache.dubbo.common.constants.CommonConstants.UNDERLINE_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

/**
 * StringUtils
 */

public final class StringUtils {

    public static final String EMPTY = "";
    public static final int INDEX_NOT_FOUND = -1;
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
    private static final Pattern KVP_PATTERN = Pattern.compile("([_.a-zA-Z0-9][-_.a-zA-Z0-9]*)[=](.*)"); //key value pair pattern.
    private static final Pattern INT_PATTERN = Pattern.compile("^\\d+$");
    private static final int PAD_LIMIT = 8192;

    private StringUtils() {
    }

    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     *
     * @param cs a CharSequence or {@code null}
     * @return CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     */
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * <p>Repeat a String {@code repeat} times to form a
     * new String.</p>
     *
     * <pre>
     * StringUtils.repeat(null, 2) = null
     * StringUtils.repeat("", 0)   = ""
     * StringUtils.repeat("", 2)   = ""
     * StringUtils.repeat("a", 3)  = "aaa"
     * StringUtils.repeat("ab", 2) = "abab"
     * StringUtils.repeat("a", -2) = ""
     * </pre>
     *
     * @param str    the String to repeat, may be null
     * @param repeat number of times to repeat str, negative treated as zero
     * @return a new String consisting of the original String repeated,
     * {@code null} if null String input
     */
    public static String repeat(final String str, final int repeat) {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return EMPTY;
        }
        final int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        if (inputLength == 1 && repeat <= PAD_LIMIT) {
            return repeat(str.charAt(0), repeat);
        }

        final int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1:
                return repeat(str.charAt(0), repeat);
            case 2:
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default:
                final StringBuilder buf = new StringBuilder(outputLength);
                for (int i = 0; i < repeat; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    /**
     * <p>Repeat a String {@code repeat} times to form a
     * new String, with a String separator injected each time. </p>
     *
     * <pre>
     * StringUtils.repeat(null, null, 2) = null
     * StringUtils.repeat(null, "x", 2)  = null
     * StringUtils.repeat("", null, 0)   = ""
     * StringUtils.repeat("", "", 2)     = ""
     * StringUtils.repeat("", "x", 3)    = "xxx"
     * StringUtils.repeat("?", ", ", 3)  = "?, ?, ?"
     * </pre>
     *
     * @param str       the String to repeat, may be null
     * @param separator the String to inject, may be null
     * @param repeat    number of times to repeat str, negative treated as zero
     * @return a new String consisting of the original String repeated,
     * {@code null} if null String input
     * @since 2.5
     */
    public static String repeat(final String str, final String separator, final int repeat) {
        if (str == null || separator == null) {
            return repeat(str, repeat);
        }
        // given that repeat(String, int) is quite optimized, better to rely on it than try and splice this into it
        final String result = repeat(str + separator, repeat);
        return removeEnd(result, separator);
    }

    /**
     * <p>Removes a substring only if it is at the end of a source string,
     * otherwise returns the source string.</p>
     *
     * <p>A {@code null} source string will return {@code null}.
     * An empty ("") source string will return the empty string.
     * A {@code null} search string will return the source string.</p>
     *
     * <pre>
     * StringUtils.removeEnd(null, *)      = null
     * StringUtils.removeEnd("", *)        = ""
     * StringUtils.removeEnd(*, null)      = *
     * StringUtils.removeEnd("www.domain.com", ".com.")  = "www.domain.com"
     * StringUtils.removeEnd("www.domain.com", ".com")   = "www.domain"
     * StringUtils.removeEnd("www.domain.com", "domain") = "www.domain.com"
     * StringUtils.removeEnd("abc", "")    = "abc"
     * </pre>
     *
     * @param str    the source String to search, may be null
     * @param remove the String to search for and remove, may be null
     * @return the substring with the string removed if found,
     * {@code null} if null String input
     */
    public static String removeEnd(final String str, final String remove) {
        if (isAnyEmpty(str, remove)) {
            return str;
        }
        if (str.endsWith(remove)) {
            return str.substring(0, str.length() - remove.length());
        }
        return str;
    }

    /**
     * <p>Returns padding using the specified delimiter repeated
     * to a given length.</p>
     *
     * <pre>
     * StringUtils.repeat('e', 0)  = ""
     * StringUtils.repeat('e', 3)  = "eee"
     * StringUtils.repeat('e', -2) = ""
     * </pre>
     *
     * <p>Note: this method doesn't not support padding with
     * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a>
     * as they require a pair of {@code char}s to be represented.
     * If you are needing to support full I18N of your applications
     * consider using {@link #repeat(String, int)} instead.
     * </p>
     *
     * @param ch     character to repeat
     * @param repeat number of times to repeat char, negative treated as zero
     * @return String with repeated character
     * @see #repeat(String, int)
     */
    public static String repeat(final char ch, final int repeat) {
        final char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    /**
     * <p>Strips any of a set of characters from the end of a String.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * An empty string ("") input returns the empty string.</p>
     *
     * <p>If the stripChars String is {@code null}, whitespace is
     * stripped as defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtils.stripEnd(null, *)          = null
     * StringUtils.stripEnd("", *)            = ""
     * StringUtils.stripEnd("abc", "")        = "abc"
     * StringUtils.stripEnd("abc", null)      = "abc"
     * StringUtils.stripEnd("  abc", null)    = "  abc"
     * StringUtils.stripEnd("abc  ", null)    = "abc"
     * StringUtils.stripEnd(" abc ", null)    = " abc"
     * StringUtils.stripEnd("  abcyx", "xyz") = "  abc"
     * StringUtils.stripEnd("120.00", ".0")   = "12"
     * </pre>
     *
     * @param str        the String to remove characters from, may be null
     * @param stripChars the set of characters to remove, null treated as whitespace
     * @return the stripped String, {@code null} if null String input
     */
    public static String stripEnd(final String str, final String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.isEmpty()) {
            return str;
        } else {
            while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != INDEX_NOT_FOUND) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    /**
     * <p>Replaces all occurrences of a String within another String.</p>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *)        = null
     * StringUtils.replace("", *, *)          = ""
     * StringUtils.replace("any", null, *)    = "any"
     * StringUtils.replace("any", *, null)    = "any"
     * StringUtils.replace("any", "", *)      = "any"
     * StringUtils.replace("aba", "a", null)  = "aba"
     * StringUtils.replace("aba", "a", "")    = "b"
     * StringUtils.replace("aba", "a", "z")   = "zbz"
     * </pre>
     *
     * @param text         text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement  the String to replace it with, may be null
     * @return the text with any replacements processed,
     * {@code null} if null String input
     * @see #replace(String text, String searchString, String replacement, int max)
     */
    public static String replace(final String text, final String searchString, final String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    /**
     * <p>Replaces a String with another String inside a larger String,
     * for the first {@code max} values of the search String.</p>
     *
     * <p>A {@code null} reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *, *)         = null
     * StringUtils.replace("", *, *, *)           = ""
     * StringUtils.replace("any", null, *, *)     = "any"
     * StringUtils.replace("any", *, null, *)     = "any"
     * StringUtils.replace("any", "", *, *)       = "any"
     * StringUtils.replace("any", *, *, 0)        = "any"
     * StringUtils.replace("abaa", "a", null, -1) = "abaa"
     * StringUtils.replace("abaa", "a", "", -1)   = "b"
     * StringUtils.replace("abaa", "a", "z", 0)   = "abaa"
     * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa"
     * StringUtils.replace("abaa", "a", "z", 2)   = "zbza"
     * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz"
     * </pre>
     *
     * @param text         text to search and replace in, may be null
     * @param searchString the String to search for, may be null
     * @param replacement  the String to replace it with, may be null
     * @param max          maximum number of values to replace, or {@code -1} if no maximum
     * @return the text with any replacements processed,
     * {@code null} if null String input
     */
    public static String replace(final String text, final String searchString, final String replacement, int max) {
        if (isAnyEmpty(text, searchString) || replacement == null || max == 0) {
            return text;
        }
        int start = 0;
        int end = text.indexOf(searchString, start);
        if (end == INDEX_NOT_FOUND) {
            return text;
        }
        final int replLength = searchString.length();
        int increase = replacement.length() - replLength;
        increase = increase < 0 ? 0 : increase;
        increase *= max < 0 ? 16 : max > 64 ? 64 : max;
        final StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end != INDEX_NOT_FOUND) {
            buf.append(text.substring(start, end)).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    public static boolean isBlank(CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * is empty string.
     *
     * @param str source string.
     * @return is empty.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * <p>Checks if the strings contain empty or null elements. <p/>
     *
     * <pre>
     * StringUtils.isNoneEmpty(null)            = false
     * StringUtils.isNoneEmpty("")              = false
     * StringUtils.isNoneEmpty(" ")             = true
     * StringUtils.isNoneEmpty("abc")           = true
     * StringUtils.isNoneEmpty("abc", "def")    = true
     * StringUtils.isNoneEmpty("abc", null)     = false
     * StringUtils.isNoneEmpty("abc", "")       = false
     * StringUtils.isNoneEmpty("abc", " ")      = true
     * </pre>
     *
     * @param ss the strings to check
     * @return {@code true} if all strings are not empty or null
     */
    public static boolean isNoneEmpty(final String... ss) {
        if (ArrayUtils.isEmpty(ss)) {
            return false;
        }
        for (final String s : ss) {
            if (isEmpty(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if the strings contain at least on empty or null element. <p/>
     *
     * <pre>
     * StringUtils.isAnyEmpty(null)            = true
     * StringUtils.isAnyEmpty("")              = true
     * StringUtils.isAnyEmpty(" ")             = false
     * StringUtils.isAnyEmpty("abc")           = false
     * StringUtils.isAnyEmpty("abc", "def")    = false
     * StringUtils.isAnyEmpty("abc", null)     = true
     * StringUtils.isAnyEmpty("abc", "")       = true
     * StringUtils.isAnyEmpty("abc", " ")      = false
     * </pre>
     *
     * @param ss the strings to check
     * @return {@code true} if at least one in the strings is empty or null
     */
    public static boolean isAnyEmpty(final String... ss) {
        return !isNoneEmpty(ss);
    }

    /**
     * is not empty string.
     *
     * @param str source string.
     * @return is not empty.
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * @param s1
     * @param s2
     * @return equals
     */
    public static boolean isEquals(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
    }

    /**
     * is integer string.
     *
     * @param str
     * @return is integer
     */
    public static boolean isInteger(String str) {
        return isNotEmpty(str) && INT_PATTERN.matcher(str).matches();
    }

    public static int parseInteger(String str) {
        return isInteger(str) ? Integer.parseInt(str) : 0;
    }

    /**
     * Returns true if s is a legal Java identifier.<p>
     * <a href="http://www.exampledepot.com/egs/java.lang/IsJavaId.html">more info.</a>
     */
    public static boolean isJavaIdentifier(String s) {
        if (isEmpty(s) || !Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isContains(String values, String value) {
        return isNotEmpty(values) && isContains(COMMA_SPLIT_PATTERN.split(values), value);
    }

    /**
     * @param values
     * @param value
     * @return contains
     */
    public static boolean isContains(String[] values, String value) {
        if (isNotEmpty(value) && ArrayUtils.isNotEmpty(values)) {
            for (String v : values) {
                if (value.equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isNumeric(String str, boolean allowDot) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        boolean hasDot = false;
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (str.charAt(i) == '.') {
                if (hasDot || !allowDot) {
                    return false;
                }
                hasDot = true;
                continue;
            }
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    /**
     * @param e
     * @return string
     */
    public static String toString(Throwable e) {
        UnsafeStringWriter w = new UnsafeStringWriter();
        PrintWriter p = new PrintWriter(w);
        p.print(e.getClass().getName());
        if (e.getMessage() != null) {
            p.print(": " + e.getMessage());
        }
        p.println();
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

    /**
     * @param msg
     * @param e
     * @return string
     */
    public static String toString(String msg, Throwable e) {
        UnsafeStringWriter w = new UnsafeStringWriter();
        w.write(msg + "\n");
        PrintWriter p = new PrintWriter(w);
        try {
            e.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

    /**
     * translate.
     *
     * @param src  source string.
     * @param from src char table.
     * @param to   target char table.
     * @return String.
     */
    public static String translate(String src, String from, String to) {
        if (isEmpty(src)) {
            return src;
        }
        StringBuilder sb = null;
        int ix;
        char c;
        for (int i = 0, len = src.length(); i < len; i++) {
            c = src.charAt(i);
            ix = from.indexOf(c);
            if (ix == -1) {
                if (sb != null) {
                    sb.append(c);
                }
            } else {
                if (sb == null) {
                    sb = new StringBuilder(len);
                    sb.append(src, 0, i);
                }
                if (ix < to.length()) {
                    sb.append(to.charAt(ix));
                }
            }
        }
        return sb == null ? src : sb.toString();
    }

    /**
     * split.
     *
     * @param ch char.
     * @return string array.
     */
    public static String[] split(String str, char ch) {
        List<String> list = null;
        char c;
        int ix = 0, len = str.length();
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            if (c == ch) {
                if (list == null) {
                    list = new ArrayList<String>();
                }
                list.add(str.substring(ix, i));
                ix = i + 1;
            }
        }
        if (ix > 0) {
            list.add(str.substring(ix));
        }
        return list == null ? EMPTY_STRING_ARRAY : (String[]) list.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * join string.
     *
     * @param array String array.
     * @return String.
     */
    public static String join(String[] array) {
        if (ArrayUtils.isEmpty(array)) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : array) {
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * join string like javascript.
     *
     * @param array String array.
     * @param split split
     * @return String.
     */
    public static String join(String[] array, char split) {
        if (ArrayUtils.isEmpty(array)) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(split);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }

    /**
     * join string like javascript.
     *
     * @param array String array.
     * @param split split
     * @return String.
     */
    public static String join(String[] array, String split) {
        if (ArrayUtils.isEmpty(array)) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(split);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static String join(Collection<String> coll, String split) {
        if (CollectionUtils.isEmpty(coll)) {
            return EMPTY;
        }

        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String s : coll) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(split);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * parse key-value pair.
     *
     * @param str           string.
     * @param itemSeparator item separator.
     * @return key-value map;
     */
    private static Map<String, String> parseKeyValuePair(String str, String itemSeparator) {
        String[] tmp = str.split(itemSeparator);
        Map<String, String> map = new HashMap<String, String>(tmp.length);
        for (int i = 0; i < tmp.length; i++) {
            Matcher matcher = KVP_PATTERN.matcher(tmp[i]);
            if (!matcher.matches()) {
                continue;
            }
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    public static String getQueryStringValue(String qs, String key) {
        Map<String, String> map = StringUtils.parseQueryString(qs);
        return map.get(key);
    }

    /**
     * parse query string to Parameters.
     *
     * @param qs query string.
     * @return Parameters instance.
     */
    public static Map<String, String> parseQueryString(String qs) {
        if (isEmpty(qs)) {
            return new HashMap<String, String>();
        }
        return parseKeyValuePair(qs, "\\&");
    }

    public static String getServiceKey(Map<String, String> ps) {
        StringBuilder buf = new StringBuilder();
        String group = ps.get(GROUP_KEY);
        if (isNotEmpty(group)) {
            buf.append(group).append("/");
        }
        buf.append(ps.get(INTERFACE_KEY));
        String version = ps.get(VERSION_KEY);
        if (isNotEmpty(group)) {
            buf.append(":").append(version);
        }
        return buf.toString();
    }

    public static String toQueryString(Map<String, String> ps) {
        StringBuilder buf = new StringBuilder();
        if (ps != null && ps.size() > 0) {
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(ps).entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (isNoneEmpty(key, value)) {
                    if (buf.length() > 0) {
                        buf.append("&");
                    }
                    buf.append(key);
                    buf.append("=");
                    buf.append(value);
                }
            }
        }
        return buf.toString();
    }

    public static String camelToSplitName(String camelName, String split) {
        if (isEmpty(camelName)) {
            return camelName;
        }
        StringBuilder buf = null;
        for (int i = 0; i < camelName.length(); i++) {
            char ch = camelName.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                if (buf == null) {
                    buf = new StringBuilder();
                    if (i > 0) {
                        buf.append(camelName.substring(0, i));
                    }
                }
                if (i > 0) {
                    buf.append(split);
                }
                buf.append(Character.toLowerCase(ch));
            } else if (buf != null) {
                buf.append(ch);
            }
        }
        return buf == null ? camelName : buf.toString();
    }

    public static String toArgumentString(Object[] args) {
        StringBuilder buf = new StringBuilder();
        for (Object arg : args) {
            if (buf.length() > 0) {
                buf.append(COMMA_SEPARATOR);
            }
            if (arg == null || ReflectUtils.isPrimitives(arg.getClass())) {
                buf.append(arg);
            } else {
                try {
                    buf.append(JSON.toJSONString(arg));
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                    buf.append(arg);
                }
            }
        }
        return buf.toString();
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static String toURLKey(String key) {
        return key.toLowerCase().replaceAll(SEPARATOR_REGEX, HIDE_KEY_PREFIX);
    }

    public static String toOSStyleKey(String key) {
        key = key.toUpperCase().replaceAll(DOT_REGEX, UNDERLINE_SEPARATOR);
        if (!key.startsWith("DUBBO_")) {
            key = "DUBBO_" + key;
        }
        return key;
    }

    public static boolean isAllUpperCase(String str) {
        if (str != null && !isEmpty(str)) {
            int sz = str.length();

            for(int i = 0; i < sz; ++i) {
                if (!Character.isUpperCase(str.charAt(i))) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
