package org.apache.dubbo.rpc.protocol.rest.util;

import org.apache.dubbo.common.utils.JsonUtils;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

public class DataParseUtils {

    public static Object StringTypeConvert(Class targetType, String value) {


        if (targetType == Boolean.class) {
            return Boolean.valueOf(value);
        }

        if (targetType == String.class) {
            return value;
        }

        if (Number.class.isAssignableFrom(targetType)) {
            return NumberUtils.parseNumber(value, targetType);
        }

        return value;

    }

    public static Object jsonConvert(Class targetType, InputStream inputStream) throws Exception {
        return JsonUtils.getJson().parseObject(inputStream, targetType);
    }


    public static Object multipartFormConvert(InputStream inputStream, Charset charset) throws Exception {
        String body = StreamUtils.copyToString(inputStream, charset);
        String[] pairs = tokenizeToStringArray(body, "&");
        Object result = MultiValueCreator.createMultiValueMap();
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx == -1) {
                MultiValueCreator.add(result, URLDecoder.decode(pair, charset.name()), null);
            } else {
                String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
                String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
                MultiValueCreator.add(result, name, value);
            }
        }

        return result;
    }

    public static Object multipartFormConvert(InputStream inputStream) throws Exception {

        return multipartFormConvert(inputStream, Charset.defaultCharset());
    }


    public static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    public static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens,
                                                 boolean ignoreEmptyTokens) {
        if (str == null) {
            return null;
        } else {
            StringTokenizer st = new StringTokenizer(str, delimiters);
            ArrayList tokens = new ArrayList();

            while (true) {
                String token;
                do {
                    if (!st.hasMoreTokens()) {
                        return toStringArray(tokens);
                    }

                    token = st.nextToken();
                    if (trimTokens) {
                        token = token.trim();
                    }
                } while (ignoreEmptyTokens && token.length() <= 0);

                tokens.add(token);
            }
        }
    }

    public static String[] toStringArray(Collection<String> collection) {
        return collection == null ? null : collection.toArray(new String[collection.size()]);
    }
}
