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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.valueOf;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DOT_REGEX;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SEPARATOR_REGEX;
import static org.apache.dubbo.common.constants.CommonConstants.UNDERLINE_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_JSON_CONVERT_EXCEPTION;

/**
 * StringUtils
 */

public final class StringUtils {

    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(StringUtils.class);
    private static final Pattern KVP_PATTERN = Pattern.compile("([_.a-zA-Z0-9][-_.a-zA-Z0-9]{0,127})\s*=\s*([^\s,]+)");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?[0-9]+(\\.[0-9]+)?$");
    private static final int PAD_LIMIT = 8192;

    private StringUtils() {
    }

    /**
     * Gets a String from an Object in a null-safe manner.
     * <p>
     * The string is constructed by converting the object via {@link String#valueOf(Object)}.
     *
     * @param obj the object to convert, may be null
     * @return the passed object as string, {@code null} if the object is null
     */
    public static String toSafeString(final Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj.getClass().isPrimitive()) {
            return String.valueOf(obj);
        }
        return obj.toString();
    }

    /**
     * join string.
     */
    public static String join(String[] array) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String s : array) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static String join(String[] array, char split) {
        return join(array, String.valueOf(split));
    }

    public static String join(String[] array, String split) {
        if (array == null || array.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (split != null && i > 0) {
                sb.append(split);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static String join(Collection<String> coll, String split) {
        if (CollectionUtils.isEmpty(coll)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (String s : coll) {
            if (!isFirst) {
                sb.append(split);
            } else {
                isFirst = false;
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static String join(String[] array, String split, int start, int stop) {
        if (ArrayUtils.isEmpty(array)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < stop; i++) {
            if (split != null && i > start) {
                sb.append(split);
            }
            sb.append(array[i]);
        }
        return sb.toString();
    }

    /**
     * Parse key-value pair strings, such as "key1:value1,key2:value2", into a Map.
     *
     * @param str key-value pair string
     * @return Map
     */
    public static Map<String, String> parseParameters(String str) {
        if (isEmpty(str)) {
            return Collections.emptyMap();
        }
        Matcher matcher = KVP_PATTERN.matcher(str);
        Map<String, String> params = new HashMap<>();
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }
        return params;
    }

    public static String encodeParameters(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (isNotEmpty(entry.getKey()) && isNotEmpty(entry.getValue())) {
                sb.append(entry.getKey()).append(':').append(entry.getValue()).append(',');
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return "[" + sb + "]";
    }

    /**
     * Check if the string is empty (null or length == 0)
     */
    public static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * <p>Checks if a String is empty ("") or null.</p>
     * <p/>
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Check if the string is not empty.
     */
    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static boolean isNoneEmpty(final String... ss) {
        if (ArrayUtils.isEmpty(ss)) {
            return false;
        }
        for (String s : ss) {
            if (isEmpty(s)) {
                return false;
            }
        }
        return true;
    }\n
    public static boolean isAnyEmpty(final String... ss) {
        if (ArrayUtils.isEmpty(ss)) {
            return true;
        }
        for (String s : ss) {
            if (isEmpty(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the string is blank (null, length == 0 or whitespace)
     */
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
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
     * Check if the string is not blank.
     */
    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    /**
     * <p>Checks if a CharSequence is not empty (""), not null and not whitespace only (" ").</p>
     * <p/>
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not empty and not whitespace only
     */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
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
     * Whether the given method name pattern contains a wildcard character.
     *
     * @param name method name or pattern, may be null
     * @return {@code true} if the name contains {@code '*'} or {@code '?'}
     */
    public static boolean hasWildcard(String name) {
        if (isEmpty(name)) {
            return false;
        }
        return name.indexOf('*') >= 0 || name.indexOf('?') >= 0;
    }

    /**
     * Match a text against a wildcard pattern without using regular expressions.
     *
     * <p>{@code '*'} matches any (possibly empty) sequence of characters and {@code '?'} matches
     * exactly one character. Matching is case-sensitive. A two-pointer scan with backtracking is used
     * so that patterns containing several {@code '*'} still resolve correctly.
     *
     * @param pattern the wildcard pattern, may be null
     * @param text    the text to match, may be null
     * @return {@code true} when the whole text matches the pattern
     */
    public static boolean isWildcardMatch(String pattern, String text) {
        if (pattern == null || text == null) {
            return false;
        }
        int p = 0;
        int t = 0;
        // Position of the last '*' in the pattern and the text index it must backtrack to.
        int starP = -1;
        int starT = 0;
        int plen = pattern.length();
        int tlen = text.length();
        while (t < tlen) {
            if (p < plen) {
                char pc = pattern.charAt(p);
                if (pc == '?' || pc == text.charAt(t)) {
                    // '?' consumes exactly one character; a literal char matches itself.
                    p++;
                    t++;
                    continue;
                }
                if (pc == '*') {
                    // Remember the star and try to match zero characters first.
                    starP = p;
                    starT = t;
                    p++;
                    continue;
                }
            }
            if (starP >= 0) {
                // Mismatch after a '*': let it consume one more character and retry.
                p = starP + 1;
                starT++;
                t = starT;
            } else {
                return false;
            }
        }
        // Any trailing '*' matches the empty remainder.
        while (p < plen && pattern.charAt(p) == '*') {
            p++;
        }
        return p == plen;
    }

    /**
     * split string.
     */
    public static String[] split(String str, char ch) {
        return split(str, ch, true, false);
    }

    public static String[] split(String str, char ch, boolean preserveAllTokens, boolean trim) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<>();
        int start = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            if (str.charAt(i) == ch) {
                if (preserveAllTokens || i > start) {
                    String sub = str.substring(start, i);
                    list.add(trim ? sub.trim() : sub);
                }
                start = i + 1;
            }
        }
        if (preserveAllTokens || len > start) {
            String sub = str.substring(start);
            list.add(trim ? sub.trim() : sub);
        }
        return list.toArray(new String[0]);
    }

    public static List<String> splitToList(String str, char separatorChar) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(split(str, separatorChar));
    }

    public static Set<String> splitToSet(String str, char separatorChar, boolean trim) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return emptySet();
        }
        return unmodifiableSet(new LinkedHashSet<>(splitToList(str, separatorChar, trim)));
    }

    public static List<String> splitToList(String str, char separatorChar, boolean trim) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(split(str, separatorChar, true, trim));
    }

    public static String[] split(String str, String splitChars) {
        return split(str, splitChars, true);
    }

    public static String[] split(String str, String splitChars, boolean preserveAllTokens) {
        if (str == null) {
            return null;
        }
        if (str.isEmpty()) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<>();
        int start = 0;
        int len = str.length();
        for (int i = 0; i < len; i++) {
            if (splitChars.indexOf(str.charAt(i)) >= 0) {
                if (preserveAllTokens || i > start) {
                    list.add(str.substring(start, i));
                }
                start = i + 1;
            }
        }
        if (preserveAllTokens || len > start) {
            list.add(str.substring(start));
        }
        return list.toArray(new String[0]);
    }

    public static String translate(String src, String from, String to) {
        if (isEmpty(src)) {
            return src;
        }
        StringBuilder sb = new StringBuilder(src.length());
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            int idx = from.indexOf(c);
            if (idx >= 0 && idx < to.length()) {
                sb.append(to.charAt(idx));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isContains(String[] values, String value) {
        if (ArrayUtils.isNotEmpty(values) && isNotEmpty(value)) {
            for (String v : values) {
                if (value.equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isContains(String str, char ch) {
        return isNotEmpty(str) && str.indexOf(ch) >= 0;
    }

    public static boolean isNotContains(String str, char ch) {
        return !isContains(str, ch);
    }

    public static boolean isNumeric(String str, boolean allowDecimal) {
        if (isEmpty(str)) {
            return false;
        }
        if (allowDecimal) {
            return NUMERIC_PATTERN.matcher(str).matches();
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNumber(String str) {
        return isNumeric(str, false);
    }

    public static int parseInteger(String str) {
        return parseInteger(str, 0);
    }

    public static int parseInteger(String str, int defaultValue) {
        if (isEmpty(str)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

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
     * Parse query string to map.
     */
    public static Map<String, String> parseQueryString(String s) {
        if (s == null || s.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new TreeMap<>();
        for (String kv : s.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0) {
                String k = kv.substring(0, i);
                String v = kv.substring(i + 1);
                map.put(k, v);
            }
        }
        return map;
    }

    public static String getQueryStringValue(String qs, String key) {
        Map<String, String> map = parseQueryString(qs);
        return map.get(key);
    }

    public static String getServiceKey(Map<String, String> ps) {
        StringBuilder buf = new StringBuilder();
        String group = ps.get(GROUP_KEY);
        if (isNotEmpty(group)) {
            buf.append(group).append('/');
        }
        buf.append(ps.get(INTERFACE_KEY));
        String version = ps.get(VERSION_KEY);
        if (isNotEmpty(version)) {
            buf.append(':').append(version);
        }
        return buf.toString();
    }

    public static String toQueryString(Map<String, String> ps) {
        StringBuilder buf = new StringBuilder();
        if (ps != null) {
            for (Map.Entry<String, String> entry : ps.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (isNotEmpty(key) && isNotEmpty(value)) {
                    buf.append(key).append('=').append(value).append('&');
                }
            }
            if (buf.length() > 0) {
                buf.setLength(buf.length() - 1);
            }
        }
        return buf.toString();
    }

    public static String camelToSplitName(String camelName, String split) {
        return convertToSplitName(camelName, split);
    }

    public static String snakeToSplitName(String snakeName, String split) {
        if (snakeName == null || snakeName.isEmpty()) {
            return snakeName;
        }
        return snakeName.replace(UNDERLINE_SEPARATOR, split);
    }

    public static String convertToSplitName(String name, String split) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '-' || ch == '_') {
                result.append(split);
                continue;
            }
            if (Character.isUpperCase(ch))
                && (i == 0 || Character.isUpperCase(name.charAt(i - 1)) == false)) {
                result.append(split);
            }
            result.append(Character.toLowerCase(ch));
        }
        return result.toString();
    }

    public static String toArgumentString(Object[] args) {
        StringBuilder buf = new StringBuilder();
        for (Object arg : args) {
            if (buf.length() > 0) {
                buf.append(',');
            }
            if (arg == null || ArgTypeUtils.isPrimitive(arg.getClass())) {
                buf.append(arg);
            } else {
                try {
                    buf.append(JsonUtils.toJson(arg));
                } catch (Exception e) {
                    logger.warn(COMMON_JSON_CONVERT_EXCEPTION, "", "", "Argument " + arg + " json convert failed.", e);
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
        return key.replace('-', '.').replace('_', '.');
    }

    public static String toOSStyleKey(String key) {
        key = key.replace('.', '_').replace('-', '_');
        return key.toUpperCase();
    }

    public static boolean isSameType(String oldType, String newType) {
        return isEquals(oldType, newType);
    }

    public static boolean isAllUpperCase(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isUpperCase(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // To commons-lang style
    // -----------------------------------------------------------------------

    /**
     * <p>Repeats a String {@code repeat} times to form a new String.</p>
     *
     * @param str    the String to repeat, may be null
     * @param repeat number of times to repeat str, negative treated as zero
     * @return a new String consisting of the original String repeated, {@code null} if null String input
     */
    public static String repeat(final String str, final int repeat) {
        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return EMPTY_STRING;
        }
        final int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        final int outputLength = inputLength * repeat;
        final StringBuilder buf = new StringBuilder(outputLength);
        for (int i = 0; i < repeat; i++) {
            buf.append(str);
        }
        return buf.toString();
    }

    /**
     * <p>Repeats a String {@code repeat} times to form a new String, with a String separator injected each time.</p>
     *
     * @param str       the String to repeat, may be null
     * @param separator the String to inject, may be null
     * @param repeat    number of times to repeat str, negative treated as zero
     * @return a new String consisting of the original String repeated, null if null String input
     */
    public static String repeat(final String str, final String separator, final int repeat) {
        if (str == null || separator == null) {
            return repeat(str, repeat);
        }
        if (repeat <= 0) {
            return EMPTY_STRING;
        }
        final int strLength = str.length();
        final int sepLength = separator.length();
        final StringBuilder buf = new StringBuilder(strLength * repeat + sepLength * (repeat - 1));
        for (int i = 0; i < repeat; i++) {
            if (i > 0) {
                buf.append(separator);
            }
            buf.append(str);
        }
        return buf.toString();
    }

    /**
     * <p>Returns padding using the specified delimiter repeated to a given length.</p>
     *
     * @param ch     character to repeat
     * @param repeat number of times to repeat char, negative treated as zero
     * @return String with repeated character
     */
    public static String repeat(final char ch, final int repeat) {
        if (repeat <= 0) {
            return EMPTY_STRING;
        }
        final char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    /**
     * <p>Strips any of a set of characters from the end of a String.</p>
     *
     * @param str        the String to remove characters from
     * @param stripChars the characters to remove, null treated as whitespace
     * @return the stripped String, {@code null} if null string input
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
     */
    public static String replace(final String text, final String searchString, final String replacement) {
        return replace(text, searchString, replacement, -1);
    }

    /**
     * <p>Replaces a String with another String inside a larger String, for the first {@code max} values of the search String.</p>
     */
    public static String replace(final String text, String searchString, final String replacement, int max) {
        if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
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
            buf.append(text, start, end).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = text.indexOf(searchString, start);
        }
        buf.append(text, start, text.length());
        return buf.toString();
    }

    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is null.
     */
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * <p>Capitalizes a String changing the first character to title case as per {@link Character#toTitleCase(int)}. No other characters are changed.</p>
     */
    public static String capitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toTitleCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            return str;
        }
        final int[] newCodePoints = new int[strLen];
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint;
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen;) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint;
            inOffset += Character.charCount(codepoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }

    /**
     * <p>Uncapitalizes a String, changing the first character to lower case as per {@link Character#toLowerCase(int)}. No other characters are changed.</p>
     */
    public static String uncapitalize(final String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        final int firstCodepoint = str.codePointAt(0);
        final int newCodePoint = Character.toLowerCase(firstCodepoint);
        if (firstCodepoint == newCodePoint) {
            return str;
        }
        final int[] newCodePoints = new int[strLen];
        int outOffset = 0;
        newCodePoints[outOffset++] = newCodePoint;
        for (int inOffset = Character.charCount(firstCodepoint); inOffset < strLen;) {
            final int codepoint = str.codePointAt(inOffset);
            newCodePoints[outOffset++] = codepoint;
            inOffset += Character.charCount(codepoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }

    /**
     * Checks if the String contains only unicode letters.
     */
    public static boolean isAlpha(final String cs) {
        if (cs == null || cs.isEmpty()) {
            return false;
        }
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetter(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the String contains only unicode letters and space (' ')
     */
    public static boolean isAlphaSpace(final String cs) {
        if (cs == null || cs.isEmpty()) {
            return false;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isLetter(cs.charAt(i)) && cs.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the String contains only unicode letters and digits.
     */
    public static boolean isAlphanumeric(final String cs) {
        if (cs == null || cs.isEmpty()) {
            return false;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isLetterOrDigit(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the String contains only unicode letters, digits or space (' ')
     */
    public static boolean isAlphanumericSpace(final String cs) {
        if (cs == null || cs.isEmpty()) {
            return false;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isLetterOrDigit(cs.charAt(i)) && cs.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the String contains only unicode digits. A decimal point is not a unicode digit and returns false.
     */
    public static boolean isDigits(final String str) {
        return isNumeric(str, false);
    }

    /**
     * Checks if the String contains only lowercase characters.
     */
    public static boolean isAllLowerCase(final String cs) {
        if (cs == null || isEmpty(cs)) {
            return false;
        }
        for (int i = 0; i < cs.length(); i++) {
            char c = cs.charAt(i);
            if (!(Character.isLowerCase(c) || !Character.isLetter(c))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the String contains only uppercase characters.
     */
    public static boolean isAllUpperCase2(final String cs) {
        if (cs == null || isEmpty(cs)) {
            return false;
        }
        for (int i = 0; i < cs.length(); i++) {
            char c = cs.charAt(i);
            if (!(Character.isUpperCase(c) || !Character.isLetter(c))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Remove all {@code remove} occurrences from within the source String.
     */
    public static String remove(final String str, final char remove) {
        if (isEmpty(str) || str.indexOf(remove) == INDEX_NOT_FOUND) {
            return str;
        }
        final char[] chars = str.toCharArray();
        int pos = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != remove) {
                chars[pos++] = chars[i];
            }
        }
        return new String(chars, 0, pos);
    }

    /**
     * Case insensitive remove
     */
    public static String removeIgnoreCase(final String str, final char remove) {
        if (isEmpty(str) || str.toLowerCase().indexOf(Character.toLowerCase(remove)) == INDEX_NOT_FOUND) {
            return str;
        }
        final char[] chars = str.toCharArray();
        int pos = 0;
        for (int i = 0; i < chars.length; i++) {
            if (Character.toLowerCase(chars[i]) != Character.toLowerCase(remove)) {
                chars[pos++] = chars[i];
            }
        }
        return new String(chars, 0, pos);
    }

    /**
     * Remove all occurrences of a substring from within the source string.
     */
    public static String remove(final String str, final String remove) {
        if (isEmpty(str) || isEmpty(remove) || !str.contains(remove)) {
            return str;
        }
        return replace(str, remove, EMPTY_STRING, -1);
    }

    /**
     * Case insensitive remove of a substring.
     */
    public static String removeIgnoreCase(final String str, final String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        return str.replaceAll("(?i)" + Pattern.quote(remove), EMPTY_STRING);
    }

    /**
     * Delete the character at the specified position.
     */
    public static String deleteCharAt(final String str, final int index) {
        if (str == null || index < 0 || index >= str.length()) {
            return str;
        }
        final char[] chars = str.toCharArray();n        final char[] newChars = new char[chars.length - 1];
        System.arraycopy(chars, 0, newChars, 0, index);
        System.arraycopy(chars, index + 1, newChars, index, chars.length - index - 1);
        return new String(newChars);
    }

    public static String substringBefore(final String str, final String separator) {
        if (isEmpty(str) || separator == null) {
            return str;
        }
        if (separator.isEmpty()) {
            return EMPTY_STRING;
        }
        final int pos = str.indexOf(separator);
        return pos == INDEX_NOT_FOUND ? EMPTY_STRING : str.substring(0, pos);
    }

    public static String substringAfter(final String str, final char separator) {
        if (isEmpty(str)) {
            return str;
        }
        final int pos = str.indexOf(separator);
        return pos == INDEX_NOT_FOUND ? EMPTY_STRING : str.substring(pos + 1);
    }

    public static String substringBetween(final String str, final String tag) {
        return substringBetween(str, tag, tag);
    }

    public static String substringBetween(final String str, final String open, final String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        final int start = str.indexOf(open);
        if (start != INDEX_NOT_FOUND) {
            final int end = str.indexOf(close, start + open.length());
            if (end != INDEX_NOT_FOUND) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    /**
     * Gets the substring after the first occurrence of a separator.
     */
    public static String substringAfterLast(final String str, final String separator) {
        if (isEmpty(str)) {
            return str;
        }
        if (isEmpty(separator)) {
            return EMPTY_STRING;
        }
        final int pos = str.lastIndexOf(separator);
        final int pos2 = pos + separator.length();
        return pos2 <= str.length() ? str.substring(pos2) : EMPTY_STRING;
    }

    public static String left(final String str, final int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return EMPTY_STRING;
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(0, len);
    }

    public static String right(final String str, final int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return EMPTY_STRING;
        }
        if (str.length() <= len) {
            return EMPTY_STRING;
        }
        return str.substring(str.length() - len);
    }

    public static String[] delimitedListToStringArray(final String str, final String delimiter) {
        return delimitedListToStringArray(str, delimiter, null);
    }

    public static String[] delimitedListToStringArray(final String str, final String delimiter, final String charsToDelete) {
        if (str == null) {
            return EMPTY_STRING_ARRAY;
        }
        if (delimiter == null) {
            return new String[] {str};
        }
        final List<String> result = new ArrayList<>();
        if (delimiter.isEmpty()) {
            result.add(deleteAny(str, charsToDelete));
            return result.toArray(EMPTY_STRING_ARRAY);
        }
        int pos = 0;
        int delPos;
        while ((delPos = str.indexOf(delimiter, pos)) != INDEX_NOT_FOUND) {
            result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
            pos = delPos + delimiter.length();
        }
        if (str.length() > 0 && pos <= str.length()) {
            result.add(deleteAny(str.substring(pos), charsToDelete));
        }
        return result.toArray(EMPTY_STRING_ARRAY);
    }

    public static String deleteAny(final String inString, final String charsToDelete) {
        if (isEmpty(inString) || isEmpty(charsToDelete)) {
            return inString;
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inString.length(); i++) {
            final char c = inString.charAt(i);
            if (charsToDelete.indexOf(c) == INDEX_NOT_FOUND) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Convert a name into camel case.
     */
    public static String camelCase(String inputName) {
        if (inputName == null || inputName.isEmpty()) {
            return inputName;
        }
        StringBuilder result = new StringBuilder();
        boolean flag = false;
        for (int i = 0; i < inputName.length(); i++) {
            char ch = inputName.charAt(i);
            if ('_' == ch) {
                flag = true;
            } else {
                if (flag) {
                    result.append(Character.toUpperCase(ch));
                    flag = false;
                } else {
                    result.append(ch);
                }
            }
        }
        return result.toString();
    }

    public static String convertToCamelCase(String inputName) {
        return camelCase(inputName);
    }

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str != null && prefix != null && str.length() >= prefix.length()
                && str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    public static boolean endsWithIgnoreCase(String str, String suffix) {
        return str != null && suffix != null && str.length() >= suffix.length()
                && str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length());
    }

    public static boolean isStartWith(String str, char ch) {
        return str != null && !str.isEmpty() && str.charAt(0) == ch;
    }

    public static boolean isEndWith(String str, char ch) {
        return str != null && !str.isEmpty() && str.charAt(str.length() - 1) == ch;
    }

    /**
     * Convert a {@code String} array into a comma delimited string.
     */
    public static String toCommaDelimitedString(String[] arr) {
        String[] arrLocal = arr == null ? new String[0] : arr;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrLocal.length; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(arrLocal[i]);
        }
        return sb.toString();
    }

    public static String toCommaDelimitedString(String... arr) {
        String[] arrLocal = arr == null ? new String[0] : arr;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrLocal.length; ++i) {
            if (i > 0) {
                sb.append(COMMA_SEPARATOR);
            }
            sb.append(arrLocal[i]);
        }
        return sb.toString();
    }

    /**
     * Convert a comma delimited string to a list.
     */
    public static List<String> commaDelimitedListToString(String str) {
        if (str == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(COMMA_SPLIT_PATTERN.split(str));
    }

    /**
     * Convert a comma delimited string to a set.
     */
    public static Set<String> commaDelimitedListToSet(String str) {
        Set<String> set = new LinkedHashSet<>();
        if (str == null) {
            return set;
        }
        String[] arr = COMMA_SPLIT_PATTERN.split(str);
        for (String s : arr) {
            set.add(s);
        }
        return set;
    }

    public static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    public static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
        if (str == null) {
            return EMPTY_STRING_ARRAY;
        }
        final List<String> tokens = new ArrayList<>();
        splitStringToCollection(str, delimiters, trimTokens, ignoreEmptyTokens, tokens);
        return tokens.toArray(EMPTY_STRING_ARRAY);
    }

    public static Set<String> tokenizeToStringSet(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
        if (str == null) {
            return emptySet();
        }
        Set<String> tokens = new LinkedHashSet<>();
        splitStringToCollection(str, delimiters, trimTokens, ignoreEmptyTokens, tokens);
        return tokens;
    }

    private static void splitStringToCollection(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens,
                                                Collection<String> tokens) {
        if (isEmpty(str)) {
            return;
        }
        final int len = str.length();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (delimiters.indexOf(c) >= 0) {
                if (word.length() > 0 || !ignoreEmptyTokens) {
                    String token = trimTokens ? word.toString().trim() : word.toString();
                    if (token.length() > 0 || !ignoreEmptyTokens) {
                        tokens.add(token);
                    }
                }
                word.setLength(0);
            } else {
                word.append(c);
            }
        }
        if (word.length() > 0 || !ignoreEmptyTokens) {
            String token = trimTokens ? word.toString().trim() : word.toString();
            if (token.length() > 0 || !ignoreEmptyTokens) {
                tokens.add(token);
            }
        }
    }

    public static String getString(String str, String defaultStr) {
        return str == null ? defaultStr : str;
    }

    public static boolean toBoolean(String str) {
        return Boolean.parseBoolean(str);
    }

    public static Boolean toBoolean(String str, Boolean defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Boolean.valueOf(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Parse the given {@code text} of the form X..Y into an int array of length 2.
     */
    public static int[] parseRange(String text) {
        if (isEmpty(text)) {
            return null;
        }
        String[] split = SEPARATOR_REGEX.split(text);
        if (split.length != 2) {
            return null;
        }
        try {
            return new int[] {Integer.parseInt(split[0].trim()), Integer.parseInt(split[1].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isNotEmpty(CharSequence sequence) {
        return sequence != null && sequence.length() > 0;
    }

    public static boolean hasText(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasLength(String text) {
        return text != null && !text.isEmpty();
    }

    public static String replace(String inString, String oldPattern, String newPattern) {
        if (isEmpty(inString) || isEmpty(oldPattern) || newPattern == null) {
            return inString;
        }
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        int index = inString.indexOf(oldPattern);
        int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString, pos, index);
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }
        sb.append(inString.substring(pos));
        return sb.toString();
    }

    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        if (index + substring.length() > str.length()) {
            return false;
        }
        for (int i = 0; i < substring.length(); i++) {
            if (str.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static int countOccurrencesOf(String str, String sub) {
        if (isEmpty(str) || isEmpty(sub)) {
            return 0;
        }
        int count = 0;
        int pos = 0;
        int idx;
        while ((idx = str.indexOf(sub, pos)) != -1) {
            ++count;
            pos = idx + sub.length();
        }
        return count;
    }

    public static String deleteAny(String inString, String charsToDelete, boolean caseInsensitive) {
        if (isEmpty(inString) || isEmpty(charsToDelete)) {
            return inString;
        }
        StringBuilder sb = new StringBuilder(inString.length());
        for (int i = 0; i < inString.length(); i++) {
            char c = inString.charAt(i);
            if (caseInsensitive) {
                if (charsToDelete.toLowerCase().indexOf(Character.toLowerCase(c)) == INDEX_NOT_FOUND) {
                    sb.append(c);
                }
            } else {
                if (charsToDelete.indexOf(c) == INDEX_NOT_FOUND) {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public static String quote(String str) {
        return "'" + str + "'";
    }

    public static String qualifyQualify(String prefix, String shortName) {
        return prefix + '.' + shortName;
    }

    public static String unqualify(String qualifiedName) {
        return unqualify(qualifiedName, '.');
    }

    public static String unqualify(String qualifiedName, char separator) {
        return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
    }

    public static String capitalize(String str, char... delimiters) {
        final int delimLen = delimiters == null ? -1 : delimiters.length;
        if (isEmpty(str) || delimLen == 0) {
            return str;
        }
        char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }
        }
        return new String(buffer);
    }

    public static String uncapitalize(String str, char... delimiters) {
        final int delimLen = delimiters == null ? -1 : delimiters.length;
        if (isEmpty(str) || delimLen == 0) {
            return str;
        }
        char[] buffer = str.toCharArray();
        boolean uncapitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
                uncapitalizeNext = true;
            } else if (uncapitalizeNext) {
                buffer[i] = Character.toLowerCase(ch);
                uncapitalizeNext = false;
            }
        }
        return new String(buffer);
    }

    private static boolean isDelimiter(char ch, char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (char delimiter : delimiters) {
            if (ch == delimiter) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts the supplied {@link CharSequence} to a {@code String}, trimming leading and trailing whitespace.
     */
    public static String trimWhitespace(final CharSequence str) {
        if (!hasLength(str)) {
            return str == null ? null : EMPTY_STRING;
        }
        StringBuilder sb = new StringBuilder(str);
        int start = 0;
        while (start < sb.length() && Character.isWhitespace(sb.charAt(start))) {
            start++;
        }
        int end = sb.length();
        while (end > start && Character.isWhitespace(sb.charAt(end - 1))) {
            end--;
        }
        return sb.substring(start, end);
    }

    public static String trimAllWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }
        int len = str.length();
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (!Character.isWhitespace(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static String trimLeadingWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    public static String trimTrailingWhitespace(String str) {
        if (!hasLength(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String trimLeadingCharacter(String str, char leadingCharacter) {
        if (!hasLength(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && sb.charAt(0) == leadingCharacter) {
            sb.deleteCharAt(0);
        }
        return sb.toString();
    }

    public static String trimTrailingCharacter(String str, char trailingCharacter) {
        if (!hasLength(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == trailingCharacter) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static boolean matchesCharacter(String str, int index, char singleCharacter) {
        return str != null && index >= 0 && index < str.length() && str.charAt(index) == singleCharacter;
    }

    public static boolean pathEquals(String path1, String path2) {
        return cleanPath(path1).equals(cleanPath(path2));
    }

    public static String cleanPath(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path.replace('\\', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return normalized;
    }

    public static String getFilename(String path) {
        if (path == null) {
            return null;
        }
        int separatorIndex = path.lastIndexOf("/");
        return separatorIndex >= 0 ? path.substring(separatorIndex + 1) : path;
    }

    public static String getFilenameExtension(String path) {
        if (path == null) {
            return null;
        }
        int extIndex = path.lastIndexOf('.');
        if (extIndex == -1) {
            return null;
        }
        for (int i = extIndex + 1; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/' || c == '\\') {
                return null;
            }
        }
        return path.substring(extIndex + 1);
    }

    public static String stripFilenameExtension(String path) {
        if (path == null) {
            return null;
        }
        int extIndex = path.lastIndexOf('.');
        if (extIndex == -1) {
            return path;
        }
        for (int i = extIndex + 1; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '/' || c == '\\') {
                return path;
            }
        }
        return path.substring(0, extIndex);
    }

    public static String applyRelativePath(String path, String relativePath) {
        int separatorIndex = path.lastIndexOf('/');
        if (separatorIndex != -1) {
            String newPath = path.substring(0, separatorIndex);
            if (!relativePath.startsWith("/")) {
                newPath += '/';
            }
            newPath += relativePath;
            return newPath;
        }
        return relativePath;
    }

    public static String[] tokenize(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    public static String toDotCase(String str) {
        return str == null ? null : str.replaceAll(SEPARATOR_REGEX.pattern(), DOT_REGEX);
    }

    public static String toLineCase(String str) {
        return str == null ? null : str.replaceAll(DOT_REGEX, SEPARATOR_REGEX.pattern());
    }

    public static boolean isTrue(String s) {
        return "true".equalsIgnoreCase(s);
    }

    public static boolean isFalse(String s) {
        return "false".equalsIgnoreCase(s);
    }

    public static boolean isBoolean(String s) {
        return isTrue(s) || isFalse(s);
    }

    public static String getDefaultValueIfEmpty(String value, String defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }

    public static <T> T getOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static int length(String str) {
        return str == null ? 0 : str.length();
    }

    public static String abbreviate(String str, int maxWidth) {
        if (str == null || maxWidth < 4) {
            return str;
        }
        if (str.length() <= maxWidth) {
            return str;
        }
        return str.substring(0, maxWidth - 3) + "...";
    }

    public static String wrap(String str, String prefix, String suffix) {
        return prefix + str + suffix;
    }

    public static String wrapIfMissing(String str, String prefix, String suffix) {
        if (str == null) {
            return str;
        }
        return (str.startsWith(prefix) ? EMPTY_STRING : prefix) + str + (str.endsWith(suffix) ? EMPTY_STRING : suffix);
    }

    public static String surround(String str, char wrapChar) {
        return wrapChar + str + wrapChar;
    }

    public static String removeStart(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        return str.startsWith(remove) ? str.substring(remove.length()) : str;
    }

    public static String removeEnd(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        return str.endsWith(remove) ? str.substring(0, str.length() - remove.length()) : str;
    }

    public static String removeStartIgnoreCase(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        return startsWithIgnoreCase(str, remove) ? str.substring(remove.length()) : str;
    }

    public static String removeEndIgnoreCase(String str, String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        return endsWithIgnoreCase(str, remove) ? str.substring(0, str.length() - remove.length()) : str;
    }

    public static String[] splitPreserveAllTokens(String str, char separatorChar) {
        return split(str, separatorChar, true, false);
    }

    public static String difference(String str1, String str2) {
        if (str1 == null) {
            return str2;
        }
        if (str2 == null) {
            return str1;
        }
        StringBuilder sb = new StringBuilder();
        Set<Character> set = new LinkedHashSet<>();
        for (int i = 0; i < str1.length(); i++) {
            set.add(str1.charAt(i));
        }
        for (int i = 0; i < str2.length(); i++) {
            if (!set.contains(str2.charAt(i))) {
                sb.append(str2.charAt(i));
            }
        }
        return sb.toString();
    }

    public static String intersection(String... strs) {
        if (strs == null || strs.length == 0) {
            return null;
        }
        Set<Character> set = toCharSet(strs[0]);
        for (int i = 1; i < strs.length; i++) {
            set.retainAll(toCharSet(strs[i]));
        }
        return set.toString();
    }

    private static Set<Character> toCharSet(String str) {
        Set<Character> set = new LinkedHashSet<>();
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                set.add(str.charAt(i));
            }
        }
        return set;
    }

    public static boolean isNumericSpace(final String cs) {
        if (cs == null) {
            return false;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isDigit(cs.charAt(i)) && cs.charAt(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    public static boolean isWhitespace(final CharSequence cs) {
        if (cs == null) {
            return false;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String defaultString(final String str) {
        return str == null ? EMPTY_STRING : str;
    }

    public static String defaultString(final String str, final String defaultStr) {
        return str == null ? defaultStr : str;
    }

    public static <T extends CharSequence> T defaultIfBlank(final T str, final T defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    public static <T extends CharSequence> T defaultIfEmpty(final T str, final T defaultStr) {
        return isEmpty(str) ? defaultStr : str;
    }

    public static String reverse(final String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    public static String reverseDelimited(final String str, final char separatorChar) {
        return reverseDelimited(str, separatorChar, false);
    }

    public static String reverseDelimited(final String str, final char separatorChar, boolean preserveAllTokens) {
        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EMPTY_STRING;
        }
        final StringBuilder buf = new StringBuilder(len);
        for (int i = len - 1; i >= 0; i--) {
            char c = str.charAt(i);
            if (c == separatorChar) {
                if (preserveAllTokens || buf.length() > 0) {
                    buf.append(separatorChar);
                }
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    public static boolean containsAny(final CharSequence cs, final CharSequence... searchCharSequences) {
        if (cs == null || searchCharSequences == null) {
            return false;
        }
        for (CharSequence search : searchCharSequences) {
            if (contains(cs, search)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final CharSequence seq, final CharSequence searchSeq) {
        if (seq == null || searchSeq == null) {
            return false;
        }
        return seq.toString().contains(searchSeq);
    }

    public static boolean containsIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        final int len = searchStr.length();
        final int max = str.length() - len;
        for (int i = 0; i <= max; i++) {
            if (str.toString().regionMatches(true, i, searchStr.toString(), 0, len)) {
                return true;
            }
        }
        return false;
    }

    public static int indexOfIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        return indexOfIgnoreCase(str, searchStr, 0);
    }

    public static int indexOfIgnoreCase(final CharSequence str, final CharSequence searchStr, int startPos) {
        if (str == null || searchStr == null) {
            return INDEX_NOT_FOUND;
        }
        if (searchStr.length() == 0) {
            return Math.max(startPos, 0);
        }
        if (startPos < 0) {
            startPos = 0;
        }
        final int slen = searchStr.length();
        final int max = str.length() - slen;
        for (int i = startPos; i <= max; i++) {
            if (str.toString().regionMatches(true, i, searchStr.toString(), 0, slen)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static int lastIndexOfIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        if (str == null || searchStr == null) {
            return INDEX_NOT_FOUND;
        }
        final int slen = searchStr.length();
        final int max = str.length() - slen;
        for (int i = max; i >= 0; i--) {
            if (str.toString().regionMatches(true, i, searchStr.toString(), 0, slen)) {
                return i;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean startsWith(final CharSequence str, final CharSequence prefix) {
        if (str == null || prefix == null) {
            return false;
        }
        return str.toString().startsWith(prefix.toString());
    }

    public static boolean endsWith(final CharSequence str, final CharSequence suffix) {
        if (str == null || suffix == null) {
            return false;
        }
        return str.toString().endsWith(suffix.toString());
    }

    public static String leftPad(final String str, final int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (isEmpty(padStr)) {
            padStr = SPACE;
        }
        final int padLen = padStr.length();
        final int strLen = str.length();
        final int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        if (padLen == 1 && pads <= PAD_LIMIT) {
            return repeat(padStr.charAt(0), pads) + str;
        }
        if (pads == padLen) {
            return padStr + str;
        } else if (pads < padLen) {
            return padStr.substring(0, pads) + str;
        } else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return new String(padding) + str;
        }
    }

    public static String rightPad(final String str, final int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (isEmpty(padStr)) {
            padStr = SPACE;
        }
        final int padLen = padStr.length();
        final int strLen = str.length();
        final int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        if (padLen == 1 && pads <= PAD_LIMIT) {
            return str + repeat(padStr.charAt(0), pads);
        }
        if (pads == padLen) {
            return str + padStr;
        } else if (pads < padLen) {
            return str + padStr.substring(0, pads);
        } else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return str + new String(padding);
        }
    }

    public static String center(String str, final int size) {
        if (str == null || size <= 0) {
            return str;
        }
        final int strLen = str.length();
        final int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        str = leftPad(str, strLen + pads / 2, SPACE);
        str = rightPad(str, size, SPACE);
        return str;
    }

    public static String upperCase(final String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase();
    }

    public static String lowerCase(final String str) {
        if (str == null) {
            return null;
        }
        return str.toLowerCase();
    }

    public static String swapCase(final String str) {
        if (isEmpty(str)) {
            return str;
        }
        final char[] buffer = str.toCharArray();
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (Character.isUpperCase(ch)) {
                buffer[i] = Character.toLowerCase(ch);
            } else if (Character.isTitleCase(ch)) {
                buffer[i] = Character.toLowerCase(ch);
            } else if (Character.isLowerCase(ch)) {
                buffer[i] = Character.toUpperCase(ch);
            }
        }
        return new String(buffer);
    }

    public static int indexOfAny(final CharSequence cs, final char... searchChars) {
        if (cs == null || searchChars == null || cs.length() == 0 || searchChars.length == 0) {
            return INDEX_NOT_FOUND;
        }
        final int csLen = cs.length();
        final int csLast = csLen - 1;
        final int searchLen = searchChars.length;
        final int searchLast = searchLen - 1;
        for (int i = 0; i < csLen; i++) {
            final char ch = cs.charAt(i);
            for (int j = 0; j < searchLen; j++) {
                if (searchChars[j] == ch) {
                    if (i < csLast && j < searchLast && Character.isHighSurrogate(ch)) {
                        if (searchChars[j + 1] == cs.charAt(i + 1)) {
                            return i;
                        }
                    } else {
                        return i;
                    }
                }
            }
        }
        return INDEX_NOT_FOUND;
    }

    public static boolean containsAny(final CharSequence cs, final char... searchChars) {
        return indexOfAny(cs, searchChars) != INDEX_NOT_FOUND;
    }

    public static String removeWhitespace(final String str) {
        if (str == null) {
            return null;
        }
        final int sz = str.length();
        final StringBuilder chs = new StringBuilder(sz);
        for (int i = 0; i < sz; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                chs.append(str.charAt(i));
            }
        }
        return chs.toString();
    }

    public static String normalizeSpace(final String str) {
        if (str == null) {
            return null;
        }
        final int size = str.length();
        final StringBuilder sb = new StringBuilder(size);
        boolean lastWasSpace = true;
        for (int i = 0; i < size; i++) {
            char c = str.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasSpace) {
                    sb.append(' ');
                }
                lastWasSpace = true;
            } else {
                sb.append(c);
                lastWasSpace = false;
            }
        }
        return sb.toString().trim();
    }

    public static String wrap(final String str, final char wrapWith) {
        if (str == null) {
            return null;
        }
        return wrapWith + str + wrapWith;
    }

    public static String unwrap(final String str, final char wrapChar) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        final char first = str.charAt(0);
        final char last = str.charAt(str.length() - 1);
        if (first == wrapChar && last == wrapChar) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    public static boolean isWrapped(final String str, char wrapChar) {
        return str != null && str.length() > 1 && str.charAt(0) == wrapChar && str.charAt(str.length() - 1) == wrapChar;
    }

    public static String truncate(final String str, int offset, int maxWidth) {
        if (str == null) {
            return null;
        }
        if (maxWidth < 0) {
            throw new IllegalArgumentException("maxWidth cannot be negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }
        if (offset > str.length()) {
            offset = str.length();
        }
        if (str.length() - offset <= maxWidth) {
            return str.substring(offset);
        }
        if (maxWidth == 0) {
            return EMPTY_STRING;
        }
        return str.substring(offset, offset + maxWidth);
    }

    public static String difference(String... strs) {
        if (strs == null || strs.length == 0) {
            return null;
        }
        Set<Character> set = toCharSet(strs[0]);
        for (int i = 1; i < strs.length; i++) {
            set.retainAll(toCharSet(strs[i]));
        }
        return set.toString();
    }

    public static boolean containsWhitespace(final CharSequence seq) {
        if (seq == null) {
            return false;
        }
        for (int i = 0; i < seq.length(); i++) {
            if (Character.isWhitespace(seq.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsWhitespace(final String str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String substring(final String str, int start) {
        if (str == null) {
            return null;
        }
        if (start < 0) {
            start = Math.max(str.length() + start, 0);
        }
        if (start > str.length()) {
            return EMPTY_STRING;
        }
        return str.substring(start);
    }

    public static String substring(final String str, int start, int end) {
        if (str == null) {
            return null;
        }
        if (start < 0) {
            start = Math.max(str.length() + start, 0);
        }
        if (end < 0) {
            end = Math.max(str.length() + end, 0);
        }
        if (end > str.length()) {
            end = str.length();
        }
        if (start > end) {
            return EMPTY_STRING;
        }
        return str.substring(start, end);
    }

    public static String overlay(final String str, String overlay, int start, int end) {
        if (str == null) {
            return null;
        }
        if (overlay == null) {
            overlay = EMPTY_STRING;
        }
        final int len = str.length();
        if (start < 0) {
            start = Math.max(start + len, 0);
        }
        if (end < 0) {
            end = Math.max(end + len, 0);
        }
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        if (start < 0) {
            start = 0;
        }
        if (start > len) {
            start = len;
        }
        if (end < 0) {
            end = 0;
        }
        if (end > len) {
            end = len;
        }
        if (start == 0 && end == len) {
            return overlay;
        }
        final StringBuilder buf = new StringBuilder();
        buf.append(str, 0, start).append(overlay).append(str, end, len);
        return buf.toString();
    }

    public static boolean isMixedCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        boolean hasLower = false, hasUpper = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isLowerCase(c)) {
                hasLower = true;
            }
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            }
            if (hasLower && hasUpper) {
                return true;
            }
        }
        return false;
    }

    public static String getNestedString(CharSequence cs, char pre, char suf) {
        if (cs == null) {
            return null;
        }
        int s = cs.toString().indexOf(pre);
        int e = cs.toString().indexOf(suf, s + 1);
        if (s < 0 || e < 0) {
            return null;
        }
        return cs.subSequence(s + 1, e).toString();
    }

    public static String convertCharset(String source, String sourceCharset, String targetCharset) {
        // Delegated; kept for API compatibility.
        return source;
    }

    private static final int INDEX_NOT_FOUND = -1;
    private static final String EMPTY_STRING = "";
    private static final String SPACE = " ";
}