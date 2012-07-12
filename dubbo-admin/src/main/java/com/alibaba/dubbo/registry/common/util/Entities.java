/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.dubbo.registry.common.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>
 * Provides HTML and XML entity utilities.
 * </p>
 * 
 * @see <a href="http://hotwired.lycos.com/webmonkey/reference/special_characters/">ISO Entities</a>
 * @see <a href="http://www.w3.org/TR/REC-html32#latin1">HTML 3.2 Character Entities for ISO Latin-1</a>
 * @see <a href="http://www.w3.org/TR/REC-html40/sgml/entities.html">HTML 4.0 Character entity references</a>
 * @see <a href="http://www.w3.org/TR/html401/charset.html#h-5.3">HTML 4.01 Character References</a>
 * @see <a href="http://www.w3.org/TR/html401/charset.html#code-position">HTML 4.01 Code positions</a>
 * 
 * @author <a href="mailto:alex@purpletech.com">Alexander Day Chaffee</a>
 * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
 * @since 2.0
 * @version $Id: Entities.java 181192 2012-06-21 05:05:47Z tony.chenl $
 */
class Entities {

    private static final String[][] BASIC_ARRAY = {{"quot", "34"}, // " - double-quote
        {"amp", "38"}, // & - ampersand
        {"lt", "60"}, // < - less-than
        {"gt", "62"}, // > - greater-than
    };

    private static final String[][] APOS_ARRAY = {{"apos", "39"}, // XML apostrophe
    };

    // package scoped for testing
    static final String[][] ISO8859_1_ARRAY = {{"nbsp", "160"}, // non-breaking space
        {"iexcl", "161"}, // inverted exclamation mark
        {"cent", "162"}, // cent sign
        {"pound", "163"}, // pound sign
        {"curren", "164"}, // currency sign
        {"yen", "165"}, // yen sign = yuan sign
        {"brvbar", "166"}, // broken bar = broken vertical bar
        {"sect", "167"}, // section sign
        {"uml", "168"}, // diaeresis = spacing diaeresis
        {"copy", "169"}, // � - copyright sign
        {"ordf", "170"}, // feminine ordinal indicator
        {"laquo", "171"}, // left-pointing double angle quotation mark = left pointing guillemet
        {"not", "172"}, // not sign
        {"shy", "173"}, // soft hyphen = discretionary hyphen
        {"reg", "174"}, // � - registered trademark sign
        {"macr", "175"}, // macron = spacing macron = overline = APL overbar
        {"deg", "176"}, // degree sign
        {"plusmn", "177"}, // plus-minus sign = plus-or-minus sign
        {"sup2", "178"}, // superscript two = superscript digit two = squared
        {"sup3", "179"}, // superscript three = superscript digit three = cubed
        {"acute", "180"}, // acute accent = spacing acute
        {"micro", "181"}, // micro sign
        {"para", "182"}, // pilcrow sign = paragraph sign
        {"middot", "183"}, // middle dot = Georgian comma = Greek middle dot
        {"cedil", "184"}, // cedilla = spacing cedilla
        {"sup1", "185"}, // superscript one = superscript digit one
        {"ordm", "186"}, // masculine ordinal indicator
        {"raquo", "187"}, // right-pointing double angle quotation mark = right pointing guillemet
        {"frac14", "188"}, // vulgar fraction one quarter = fraction one quarter
        {"frac12", "189"}, // vulgar fraction one half = fraction one half
        {"frac34", "190"}, // vulgar fraction three quarters = fraction three quarters
        {"iquest", "191"}, // inverted question mark = turned question mark
        {"Agrave", "192"}, // � - uppercase A, grave accent
        {"Aacute", "193"}, // � - uppercase A, acute accent
        {"Acirc", "194"}, // � - uppercase A, circumflex accent
        {"Atilde", "195"}, // � - uppercase A, tilde
        {"Auml", "196"}, // � - uppercase A, umlaut
        {"Aring", "197"}, // � - uppercase A, ring
        {"AElig", "198"}, // � - uppercase AE
        {"Ccedil", "199"}, // � - uppercase C, cedilla
        {"Egrave", "200"}, // � - uppercase E, grave accent
        {"Eacute", "201"}, // � - uppercase E, acute accent
        {"Ecirc", "202"}, // � - uppercase E, circumflex accent
        {"Euml", "203"}, // � - uppercase E, umlaut
        {"Igrave", "204"}, // � - uppercase I, grave accent
        {"Iacute", "205"}, // � - uppercase I, acute accent
        {"Icirc", "206"}, // � - uppercase I, circumflex accent
        {"Iuml", "207"}, // � - uppercase I, umlaut
        {"ETH", "208"}, // � - uppercase Eth, Icelandic
        {"Ntilde", "209"}, // � - uppercase N, tilde
        {"Ograve", "210"}, // � - uppercase O, grave accent
        {"Oacute", "211"}, // � - uppercase O, acute accent
        {"Ocirc", "212"}, // � - uppercase O, circumflex accent
        {"Otilde", "213"}, // � - uppercase O, tilde
        {"Ouml", "214"}, // � - uppercase O, umlaut
        {"times", "215"}, // multiplication sign
        {"Oslash", "216"}, // � - uppercase O, slash
        {"Ugrave", "217"}, // � - uppercase U, grave accent
        {"Uacute", "218"}, // � - uppercase U, acute accent
        {"Ucirc", "219"}, // � - uppercase U, circumflex accent
        {"Uuml", "220"}, // � - uppercase U, umlaut
        {"Yacute", "221"}, // � - uppercase Y, acute accent
        {"THORN", "222"}, // � - uppercase THORN, Icelandic
        {"szlig", "223"}, // � - lowercase sharps, German
        {"agrave", "224"}, // � - lowercase a, grave accent
        {"aacute", "225"}, // � - lowercase a, acute accent
        {"acirc", "226"}, // � - lowercase a, circumflex accent
        {"atilde", "227"}, // � - lowercase a, tilde
        {"auml", "228"}, // � - lowercase a, umlaut
        {"aring", "229"}, // � - lowercase a, ring
        {"aelig", "230"}, // � - lowercase ae
        {"ccedil", "231"}, // � - lowercase c, cedilla
        {"egrave", "232"}, // � - lowercase e, grave accent
        {"eacute", "233"}, // � - lowercase e, acute accent
        {"ecirc", "234"}, // � - lowercase e, circumflex accent
        {"euml", "235"}, // � - lowercase e, umlaut
        {"igrave", "236"}, // � - lowercase i, grave accent
        {"iacute", "237"}, // � - lowercase i, acute accent
        {"icirc", "238"}, // � - lowercase i, circumflex accent
        {"iuml", "239"}, // � - lowercase i, umlaut
        {"eth", "240"}, // � - lowercase eth, Icelandic
        {"ntilde", "241"}, // � - lowercase n, tilde
        {"ograve", "242"}, // � - lowercase o, grave accent
        {"oacute", "243"}, // � - lowercase o, acute accent
        {"ocirc", "244"}, // � - lowercase o, circumflex accent
        {"otilde", "245"}, // � - lowercase o, tilde
        {"ouml", "246"}, // � - lowercase o, umlaut
        {"divide", "247"}, // division sign
        {"oslash", "248"}, // � - lowercase o, slash
        {"ugrave", "249"}, // � - lowercase u, grave accent
        {"uacute", "250"}, // � - lowercase u, acute accent
        {"ucirc", "251"}, // � - lowercase u, circumflex accent
        {"uuml", "252"}, // � - lowercase u, umlaut
        {"yacute", "253"}, // � - lowercase y, acute accent
        {"thorn", "254"}, // � - lowercase thorn, Icelandic
        {"yuml", "255"}, // � - lowercase y, umlaut
    };

    // http://www.w3.org/TR/REC-html40/sgml/entities.html
    // package scoped for testing
    static final String[][] HTML40_ARRAY = {
    // <!-- Latin Extended-B -->
        {"fnof", "402"}, // latin small f with hook = function= florin, U+0192 ISOtech -->
        // <!-- Greek -->
        {"Alpha", "913"}, // greek capital letter alpha, U+0391 -->
        {"Beta", "914"}, // greek capital letter beta, U+0392 -->
        {"Gamma", "915"}, // greek capital letter gamma,U+0393 ISOgrk3 -->
        {"Delta", "916"}, // greek capital letter delta,U+0394 ISOgrk3 -->
        {"Epsilon", "917"}, // greek capital letter epsilon, U+0395 -->
        {"Zeta", "918"}, // greek capital letter zeta, U+0396 -->
        {"Eta", "919"}, // greek capital letter eta, U+0397 -->
        {"Theta", "920"}, // greek capital letter theta,U+0398 ISOgrk3 -->
        {"Iota", "921"}, // greek capital letter iota, U+0399 -->
        {"Kappa", "922"}, // greek capital letter kappa, U+039A -->
        {"Lambda", "923"}, // greek capital letter lambda,U+039B ISOgrk3 -->
        {"Mu", "924"}, // greek capital letter mu, U+039C -->
        {"Nu", "925"}, // greek capital letter nu, U+039D -->
        {"Xi", "926"}, // greek capital letter xi, U+039E ISOgrk3 -->
        {"Omicron", "927"}, // greek capital letter omicron, U+039F -->
        {"Pi", "928"}, // greek capital letter pi, U+03A0 ISOgrk3 -->
        {"Rho", "929"}, // greek capital letter rho, U+03A1 -->
        // <!-- there is no Sigmaf, and no U+03A2 character either -->
        {"Sigma", "931"}, // greek capital letter sigma,U+03A3 ISOgrk3 -->
        {"Tau", "932"}, // greek capital letter tau, U+03A4 -->
        {"Upsilon", "933"}, // greek capital letter upsilon,U+03A5 ISOgrk3 -->
        {"Phi", "934"}, // greek capital letter phi,U+03A6 ISOgrk3 -->
        {"Chi", "935"}, // greek capital letter chi, U+03A7 -->
        {"Psi", "936"}, // greek capital letter psi,U+03A8 ISOgrk3 -->
        {"Omega", "937"}, // greek capital letter omega,U+03A9 ISOgrk3 -->
        {"alpha", "945"}, // greek small letter alpha,U+03B1 ISOgrk3 -->
        {"beta", "946"}, // greek small letter beta, U+03B2 ISOgrk3 -->
        {"gamma", "947"}, // greek small letter gamma,U+03B3 ISOgrk3 -->
        {"delta", "948"}, // greek small letter delta,U+03B4 ISOgrk3 -->
        {"epsilon", "949"}, // greek small letter epsilon,U+03B5 ISOgrk3 -->
        {"zeta", "950"}, // greek small letter zeta, U+03B6 ISOgrk3 -->
        {"eta", "951"}, // greek small letter eta, U+03B7 ISOgrk3 -->
        {"theta", "952"}, // greek small letter theta,U+03B8 ISOgrk3 -->
        {"iota", "953"}, // greek small letter iota, U+03B9 ISOgrk3 -->
        {"kappa", "954"}, // greek small letter kappa,U+03BA ISOgrk3 -->
        {"lambda", "955"}, // greek small letter lambda,U+03BB ISOgrk3 -->
        {"mu", "956"}, // greek small letter mu, U+03BC ISOgrk3 -->
        {"nu", "957"}, // greek small letter nu, U+03BD ISOgrk3 -->
        {"xi", "958"}, // greek small letter xi, U+03BE ISOgrk3 -->
        {"omicron", "959"}, // greek small letter omicron, U+03BF NEW -->
        {"pi", "960"}, // greek small letter pi, U+03C0 ISOgrk3 -->
        {"rho", "961"}, // greek small letter rho, U+03C1 ISOgrk3 -->
        {"sigmaf", "962"}, // greek small letter final sigma,U+03C2 ISOgrk3 -->
        {"sigma", "963"}, // greek small letter sigma,U+03C3 ISOgrk3 -->
        {"tau", "964"}, // greek small letter tau, U+03C4 ISOgrk3 -->
        {"upsilon", "965"}, // greek small letter upsilon,U+03C5 ISOgrk3 -->
        {"phi", "966"}, // greek small letter phi, U+03C6 ISOgrk3 -->
        {"chi", "967"}, // greek small letter chi, U+03C7 ISOgrk3 -->
        {"psi", "968"}, // greek small letter psi, U+03C8 ISOgrk3 -->
        {"omega", "969"}, // greek small letter omega,U+03C9 ISOgrk3 -->
        {"thetasym", "977"}, // greek small letter theta symbol,U+03D1 NEW -->
        {"upsih", "978"}, // greek upsilon with hook symbol,U+03D2 NEW -->
        {"piv", "982"}, // greek pi symbol, U+03D6 ISOgrk3 -->
        // <!-- General Punctuation -->
        {"bull", "8226"}, // bullet = black small circle,U+2022 ISOpub -->
        // <!-- bullet is NOT the same as bullet operator, U+2219 -->
        {"hellip", "8230"}, // horizontal ellipsis = three dot leader,U+2026 ISOpub -->
        {"prime", "8242"}, // prime = minutes = feet, U+2032 ISOtech -->
        {"Prime", "8243"}, // double prime = seconds = inches,U+2033 ISOtech -->
        {"oline", "8254"}, // overline = spacing overscore,U+203E NEW -->
        {"frasl", "8260"}, // fraction slash, U+2044 NEW -->
        // <!-- Letterlike Symbols -->
        {"weierp", "8472"}, // script capital P = power set= Weierstrass p, U+2118 ISOamso -->
        {"image", "8465"}, // blackletter capital I = imaginary part,U+2111 ISOamso -->
        {"real", "8476"}, // blackletter capital R = real part symbol,U+211C ISOamso -->
        {"trade", "8482"}, // trade mark sign, U+2122 ISOnum -->
        {"alefsym", "8501"}, // alef symbol = first transfinite cardinal,U+2135 NEW -->
        // <!-- alef symbol is NOT the same as hebrew letter alef,U+05D0 although the
        // same glyph could be used to depict both characters -->
        // <!-- Arrows -->
        {"larr", "8592"}, // leftwards arrow, U+2190 ISOnum -->
        {"uarr", "8593"}, // upwards arrow, U+2191 ISOnum-->
        {"rarr", "8594"}, // rightwards arrow, U+2192 ISOnum -->
        {"darr", "8595"}, // downwards arrow, U+2193 ISOnum -->
        {"harr", "8596"}, // left right arrow, U+2194 ISOamsa -->
        {"crarr", "8629"}, // downwards arrow with corner leftwards= carriage return, U+21B5 NEW -->
        {"lArr", "8656"}, // leftwards double arrow, U+21D0 ISOtech -->
        // <!-- ISO 10646 does not say that lArr is the same as the 'is implied by'
        // arrow but also does not have any other character for that function.
        // So ? lArr canbe used for 'is implied by' as ISOtech suggests -->
        {"uArr", "8657"}, // upwards double arrow, U+21D1 ISOamsa -->
        {"rArr", "8658"}, // rightwards double arrow,U+21D2 ISOtech -->
        // <!-- ISO 10646 does not say this is the 'implies' character but does not
        // have another character with this function so ?rArr can be used for
        // 'implies' as ISOtech suggests -->
        {"dArr", "8659"}, // downwards double arrow, U+21D3 ISOamsa -->
        {"hArr", "8660"}, // left right double arrow,U+21D4 ISOamsa -->
        // <!-- Mathematical Operators -->
        {"forall", "8704"}, // for all, U+2200 ISOtech -->
        {"part", "8706"}, // partial differential, U+2202 ISOtech -->
        {"exist", "8707"}, // there exists, U+2203 ISOtech -->
        {"empty", "8709"}, // empty set = null set = diameter,U+2205 ISOamso -->
        {"nabla", "8711"}, // nabla = backward difference,U+2207 ISOtech -->
        {"isin", "8712"}, // element of, U+2208 ISOtech -->
        {"notin", "8713"}, // not an element of, U+2209 ISOtech -->
        {"ni", "8715"}, // contains as member, U+220B ISOtech -->
        // <!-- should there be a more memorable name than 'ni'? -->
        {"prod", "8719"}, // n-ary product = product sign,U+220F ISOamsb -->
        // <!-- prod is NOT the same character as U+03A0 'greek capital letter pi'
        // though the same glyph might be used for both -->
        {"sum", "8721"}, // n-ary summation, U+2211 ISOamsb -->
        // <!-- sum is NOT the same character as U+03A3 'greek capital letter sigma'
        // though the same glyph might be used for both -->
        {"minus", "8722"}, // minus sign, U+2212 ISOtech -->
        {"lowast", "8727"}, // asterisk operator, U+2217 ISOtech -->
        {"radic", "8730"}, // square root = radical sign,U+221A ISOtech -->
        {"prop", "8733"}, // proportional to, U+221D ISOtech -->
        {"infin", "8734"}, // infinity, U+221E ISOtech -->
        {"ang", "8736"}, // angle, U+2220 ISOamso -->
        {"and", "8743"}, // logical and = wedge, U+2227 ISOtech -->
        {"or", "8744"}, // logical or = vee, U+2228 ISOtech -->
        {"cap", "8745"}, // intersection = cap, U+2229 ISOtech -->
        {"cup", "8746"}, // union = cup, U+222A ISOtech -->
        {"int", "8747"}, // integral, U+222B ISOtech -->
        {"there4", "8756"}, // therefore, U+2234 ISOtech -->
        {"sim", "8764"}, // tilde operator = varies with = similar to,U+223C ISOtech -->
        // <!-- tilde operator is NOT the same character as the tilde, U+007E,although
        // the same glyph might be used to represent both -->
        {"cong", "8773"}, // approximately equal to, U+2245 ISOtech -->
        {"asymp", "8776"}, // almost equal to = asymptotic to,U+2248 ISOamsr -->
        {"ne", "8800"}, // not equal to, U+2260 ISOtech -->
        {"equiv", "8801"}, // identical to, U+2261 ISOtech -->
        {"le", "8804"}, // less-than or equal to, U+2264 ISOtech -->
        {"ge", "8805"}, // greater-than or equal to,U+2265 ISOtech -->
        {"sub", "8834"}, // subset of, U+2282 ISOtech -->
        {"sup", "8835"}, // superset of, U+2283 ISOtech -->
        // <!-- note that nsup, 'not a superset of, U+2283' is not covered by the
        // Symbol font encoding and is not included. Should it be, for symmetry?
        // It is in ISOamsn --> <!ENTITY nsub", "8836"},
        // not a subset of, U+2284 ISOamsn -->
        {"sube", "8838"}, // subset of or equal to, U+2286 ISOtech -->
        {"supe", "8839"}, // superset of or equal to,U+2287 ISOtech -->
        {"oplus", "8853"}, // circled plus = direct sum,U+2295 ISOamsb -->
        {"otimes", "8855"}, // circled times = vector product,U+2297 ISOamsb -->
        {"perp", "8869"}, // up tack = orthogonal to = perpendicular,U+22A5 ISOtech -->
        {"sdot", "8901"}, // dot operator, U+22C5 ISOamsb -->
        // <!-- dot operator is NOT the same character as U+00B7 middle dot -->
        // <!-- Miscellaneous Technical -->
        {"lceil", "8968"}, // left ceiling = apl upstile,U+2308 ISOamsc -->
        {"rceil", "8969"}, // right ceiling, U+2309 ISOamsc -->
        {"lfloor", "8970"}, // left floor = apl downstile,U+230A ISOamsc -->
        {"rfloor", "8971"}, // right floor, U+230B ISOamsc -->
        {"lang", "9001"}, // left-pointing angle bracket = bra,U+2329 ISOtech -->
        // <!-- lang is NOT the same character as U+003C 'less than' or U+2039 'single left-pointing angle quotation
        // mark' -->
        {"rang", "9002"}, // right-pointing angle bracket = ket,U+232A ISOtech -->
        // <!-- rang is NOT the same character as U+003E 'greater than' or U+203A
        // 'single right-pointing angle quotation mark' -->
        // <!-- Geometric Shapes -->
        {"loz", "9674"}, // lozenge, U+25CA ISOpub -->
        // <!-- Miscellaneous Symbols -->
        {"spades", "9824"}, // black spade suit, U+2660 ISOpub -->
        // <!-- black here seems to mean filled as opposed to hollow -->
        {"clubs", "9827"}, // black club suit = shamrock,U+2663 ISOpub -->
        {"hearts", "9829"}, // black heart suit = valentine,U+2665 ISOpub -->
        {"diams", "9830"}, // black diamond suit, U+2666 ISOpub -->

        // <!-- Latin Extended-A -->
        {"OElig", "338"}, // -- latin capital ligature OE,U+0152 ISOlat2 -->
        {"oelig", "339"}, // -- latin small ligature oe, U+0153 ISOlat2 -->
        // <!-- ligature is a misnomer, this is a separate character in some languages -->
        {"Scaron", "352"}, // -- latin capital letter S with caron,U+0160 ISOlat2 -->
        {"scaron", "353"}, // -- latin small letter s with caron,U+0161 ISOlat2 -->
        {"Yuml", "376"}, // -- latin capital letter Y with diaeresis,U+0178 ISOlat2 -->
        // <!-- Spacing Modifier Letters -->
        {"circ", "710"}, // -- modifier letter circumflex accent,U+02C6 ISOpub -->
        {"tilde", "732"}, // small tilde, U+02DC ISOdia -->
        // <!-- General Punctuation -->
        {"ensp", "8194"}, // en space, U+2002 ISOpub -->
        {"emsp", "8195"}, // em space, U+2003 ISOpub -->
        {"thinsp", "8201"}, // thin space, U+2009 ISOpub -->
        {"zwnj", "8204"}, // zero width non-joiner,U+200C NEW RFC 2070 -->
        {"zwj", "8205"}, // zero width joiner, U+200D NEW RFC 2070 -->
        {"lrm", "8206"}, // left-to-right mark, U+200E NEW RFC 2070 -->
        {"rlm", "8207"}, // right-to-left mark, U+200F NEW RFC 2070 -->
        {"ndash", "8211"}, // en dash, U+2013 ISOpub -->
        {"mdash", "8212"}, // em dash, U+2014 ISOpub -->
        {"lsquo", "8216"}, // left single quotation mark,U+2018 ISOnum -->
        {"rsquo", "8217"}, // right single quotation mark,U+2019 ISOnum -->
        {"sbquo", "8218"}, // single low-9 quotation mark, U+201A NEW -->
        {"ldquo", "8220"}, // left double quotation mark,U+201C ISOnum -->
        {"rdquo", "8221"}, // right double quotation mark,U+201D ISOnum -->
        {"bdquo", "8222"}, // double low-9 quotation mark, U+201E NEW -->
        {"dagger", "8224"}, // dagger, U+2020 ISOpub -->
        {"Dagger", "8225"}, // double dagger, U+2021 ISOpub -->
        {"permil", "8240"}, // per mille sign, U+2030 ISOtech -->
        {"lsaquo", "8249"}, // single left-pointing angle quotation mark,U+2039 ISO proposed -->
        // <!-- lsaquo is proposed but not yet ISO standardized -->
        {"rsaquo", "8250"}, // single right-pointing angle quotation mark,U+203A ISO proposed -->
        // <!-- rsaquo is proposed but not yet ISO standardized -->
        {"euro", "8364"}, // -- euro sign, U+20AC NEW -->
    };

    /**
     * <p>
     * The set of entities supported by standard XML.
     * </p>
     */
    public static final Entities XML;

    /**
     * <p>
     * The set of entities supported by HTML 3.2.
     * </p>
     */
    public static final Entities HTML32;

    /**
     * <p>
     * The set of entities supported by HTML 4.0.
     * </p>
     */
    public static final Entities HTML40;

    static {
        XML = new Entities();
        XML.addEntities(BASIC_ARRAY);
        XML.addEntities(APOS_ARRAY);
    }

    static {
        HTML32 = new Entities();
        HTML32.addEntities(BASIC_ARRAY);
        HTML32.addEntities(ISO8859_1_ARRAY);
    }

    static {
        HTML40 = new Entities();
        fillWithHtml40Entities(HTML40);
    }

    /**
     * <p>
     * Fills the specified entities instance with HTML 40 entities.
     * </p>
     * 
     * @param entities
     *            the instance to be filled.
     */
    static void fillWithHtml40Entities(Entities entities) {
        entities.addEntities(BASIC_ARRAY);
        entities.addEntities(ISO8859_1_ARRAY);
        entities.addEntities(HTML40_ARRAY);
    }

    static interface EntityMap {
        /**
         * <p>
         * Add an entry to this entity map.
         * </p>
         * 
         * @param name
         *            the entity name
         * @param value
         *            the entity value
         */
        void add(String name, int value);

        /**
         * <p>
         * Returns the name of the entity identified by the specified value.
         * </p>
         * 
         * @param value
         *            the value to locate
         * @return entity name associated with the specified value
         */
        String name(int value);

        /**
         * <p>
         * Returns the value of the entity identified by the specified name.
         * </p>
         * 
         * @param name
         *            the name to locate
         * @return entity value associated with the specified name
         */
        int value(String name);
    }

    static class PrimitiveEntityMap implements EntityMap {
        private Map mapNameToValue = new HashMap();

        private IntHashMap mapValueToName = new IntHashMap();

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            mapNameToValue.put(name, Integer.valueOf(value));
            mapValueToName.put(value, name);
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            return (String) mapValueToName.get(value);
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            Object value = mapNameToValue.get(name);
            if (value == null) {
                return -1;
            }
            return ((Integer) value).intValue();
        }
    }

    static abstract class MapIntMap implements Entities.EntityMap {
        protected Map mapNameToValue;

        protected Map mapValueToName;

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            mapNameToValue.put(name, Integer.valueOf(value));
            mapValueToName.put(Integer.valueOf(value), name);
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            return (String) mapValueToName.get(Integer.valueOf(value));
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            Object value = mapNameToValue.get(name);
            if (value == null) {
                return -1;
            }
            return ((Integer) value).intValue();
        }
    }

    static class HashEntityMap extends MapIntMap {
        /**
         * Constructs a new instance of <code>HashEntityMap</code>.
         */
        public HashEntityMap() {
            mapNameToValue = new HashMap();
            mapValueToName = new HashMap();
        }
    }

    static class TreeEntityMap extends MapIntMap {
        /**
         * Constructs a new instance of <code>TreeEntityMap</code>.
         */
        public TreeEntityMap() {
            mapNameToValue = new TreeMap();
            mapValueToName = new TreeMap();
        }
    }

    static class LookupEntityMap extends PrimitiveEntityMap {
        private String[] lookupTable;

        private int LOOKUP_TABLE_SIZE = 256;

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            if (value < LOOKUP_TABLE_SIZE) {
                return lookupTable()[value];
            }
            return super.name(value);
        }

        /**
         * <p>
         * Returns the lookup table for this entity map. The lookup table is created if it has not been previously.
         * </p>
         * 
         * @return the lookup table
         */
        private String[] lookupTable() {
            if (lookupTable == null) {
                createLookupTable();
            }
            return lookupTable;
        }

        /**
         * <p>
         * Creates an entity lookup table of LOOKUP_TABLE_SIZE elements, initialized with entity names.
         * </p>
         */
        private void createLookupTable() {
            lookupTable = new String[LOOKUP_TABLE_SIZE];
            for (int i = 0; i < LOOKUP_TABLE_SIZE; ++i) {
                lookupTable[i] = super.name(i);
            }
        }
    }

    static class ArrayEntityMap implements EntityMap {
        protected int growBy = 100;

        protected int size = 0;

        protected String[] names;

        protected int[] values;

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code>.
         */
        public ArrayEntityMap() {
            names = new String[growBy];
            values = new int[growBy];
        }

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code> specifying the size by which the array should
         * grow.
         * 
         * @param growBy
         *            array will be initialized to and will grow by this amount
         */
        public ArrayEntityMap(int growBy) {
            this.growBy = growBy;
            names = new String[growBy];
            values = new int[growBy];
        }

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            ensureCapacity(size + 1);
            names[size] = name;
            values[size] = value;
            size++;
        }

        /**
         * Verifies the capacity of the entity array, adjusting the size if necessary.
         * 
         * @param capacity
         *            size the array should be
         */
        protected void ensureCapacity(int capacity) {
            if (capacity > names.length) {
                int newSize = Math.max(capacity, size + growBy);
                String[] newNames = new String[newSize];
                System.arraycopy(names, 0, newNames, 0, size);
                names = newNames;
                int[] newValues = new int[newSize];
                System.arraycopy(values, 0, newValues, 0, size);
                values = newValues;
            }
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            for (int i = 0; i < size; ++i) {
                if (values[i] == value) {
                    return names[i];
                }
            }
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public int value(String name) {
            for (int i = 0; i < size; ++i) {
                if (names[i].equals(name)) {
                    return values[i];
                }
            }
            return -1;
        }
    }

    static class BinaryEntityMap extends ArrayEntityMap {

        /**
         * Constructs a new instance of <code>BinaryEntityMap</code>.
         */
        public BinaryEntityMap() {
            super();
        }

        /**
         * Constructs a new instance of <code>ArrayEntityMap</code> specifying the size by which the underlying array
         * should grow.
         * 
         * @param growBy
         *            array will be initialized to and will grow by this amount
         */
        public BinaryEntityMap(int growBy) {
            super(growBy);
        }

        /**
         * Performs a binary search of the entity array for the specified key. This method is based on code in
         * {@link java.util.Arrays}.
         * 
         * @param key
         *            the key to be found
         * @return the index of the entity array matching the specified key
         */
        private int binarySearch(int key) {
            int low = 0;
            int high = size - 1;

            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midVal = values[mid];

                if (midVal < key) {
                    low = mid + 1;
                } else if (midVal > key) {
                    high = mid - 1;
                } else {
                    return mid; // key found
                }
            }
            return -(low + 1); // key not found.
        }

        /**
         * {@inheritDoc}
         */
        public void add(String name, int value) {
            ensureCapacity(size + 1);
            int insertAt = binarySearch(value);
            if (insertAt > 0) {
                return; // note: this means you can't insert the same value twice
            }
            insertAt = -(insertAt + 1); // binarySearch returns it negative and off-by-one
            System.arraycopy(values, insertAt, values, insertAt + 1, size - insertAt);
            values[insertAt] = value;
            System.arraycopy(names, insertAt, names, insertAt + 1, size - insertAt);
            names[insertAt] = name;
            size++;
        }

        /**
         * {@inheritDoc}
         */
        public String name(int value) {
            int index = binarySearch(value);
            if (index < 0) {
                return null;
            }
            return names[index];
        }
    }

    // package scoped for testing
    EntityMap map = new Entities.LookupEntityMap();

    /**
     * <p>
     * Adds entities to this entity.
     * </p>
     * 
     * @param entityArray
     *            array of entities to be added
     */
    public void addEntities(String[][] entityArray) {
        for (int i = 0; i < entityArray.length; ++i) {
            addEntity(entityArray[i][0], Integer.parseInt(entityArray[i][1]));
        }
    }

    /**
     * <p>
     * Add an entity to this entity.
     * </p>
     * 
     * @param name
     *            name of the entity
     * @param value
     *            vale of the entity
     */
    public void addEntity(String name, int value) {
        map.add(name, value);
    }

    /**
     * <p>
     * Returns the name of the entity identified by the specified value.
     * </p>
     * 
     * @param value
     *            the value to locate
     * @return entity name associated with the specified value
     */
    public String entityName(int value) {
        return map.name(value);
    }

    /**
     * <p>
     * Returns the value of the entity identified by the specified name.
     * </p>
     * 
     * @param name
     *            the name to locate
     * @return entity value associated with the specified name
     */
    public int entityValue(String name) {
        return map.value(name);
    }

    /**
     * <p>
     * Escapes the characters in a <code>String</code>.
     * </p>
     * 
     * <p>
     * For example, if you have called addEntity(&quot;foo&quot;, 0xA1), escape(&quot;\u00A1&quot;) will return
     * &quot;&amp;foo;&quot;
     * </p>
     * 
     * @param str
     *            The <code>String</code> to escape.
     * @return A new escaped <code>String</code>.
     */
    public String escape(String str) {
        StringWriter stringWriter = createStringWriter(str);
        try {
            this.escape(stringWriter, str);
        } catch (IOException e) {
            // This should never happen because ALL the StringWriter methods called by #escape(Writer, String) do not
            // throw IOExceptions.
            throw new IllegalStateException(e);
        }
        return stringWriter.toString();
    }

    /**
     * <p>
     * Escapes the characters in the <code>String</code> passed and writes the result to the <code>Writer</code>
     * passed.
     * </p>
     * 
     * @param writer
     *            The <code>Writer</code> to write the results of the escaping to. Assumed to be a non-null value.
     * @param str
     *            The <code>String</code> to escape. Assumed to be a non-null value.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from calls to the {@link Writer#write(int)}
     *             methods.
     * 
     * @see #escape(String)
     * @see Writer
     */
    public void escape(Writer writer, String str) throws IOException {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            String entityName = this.entityName(c);
            if (entityName == null) {
                if (c > 0x7F) {
                    writer.write("&#");
                    writer.write(Integer.toString(c, 10));
                    writer.write(';');
                } else {
                    writer.write(c);
                }
            } else {
                writer.write('&');
                writer.write(entityName);
                writer.write(';');
            }
        }
    }

    /**
     * <p>
     * Unescapes the entities in a <code>String</code>.
     * </p>
     * 
     * <p>
     * For example, if you have called addEntity(&quot;foo&quot;, 0xA1), unescape(&quot;&amp;foo;&quot;) will return
     * &quot;\u00A1&quot;
     * </p>
     * 
     * @param str
     *            The <code>String</code> to escape.
     * @return A new escaped <code>String</code>.
     */
    public String unescape(String str) {
        int firstAmp = str.indexOf('&');
        if (firstAmp < 0) {
            return str;
        } else {
            StringWriter stringWriter = createStringWriter(str);
            try {
                this.doUnescape(stringWriter, str, firstAmp);
            } catch (IOException e) {
                // This should never happen because ALL the StringWriter methods called by #escape(Writer, String) 
                // do not throw IOExceptions.
                throw new IllegalStateException(e);
            }
            return stringWriter.toString();
        }
    }

    /**
     * Make the StringWriter 10% larger than the source String to avoid growing the writer
     *
     * @param str The source string
     * @return A newly created StringWriter
     */
    private StringWriter createStringWriter(String str) {
        return new StringWriter((int) (str.length() + (str.length() * 0.1)));
    }

    /**
     * <p>
     * Unescapes the escaped entities in the <code>String</code> passed and writes the result to the
     * <code>Writer</code> passed.
     * </p>
     * 
     * @param writer
     *            The <code>Writer</code> to write the results to; assumed to be non-null.
     * @param str
     *            The source <code>String</code> to unescape; assumed to be non-null.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from calls to the {@link Writer#write(int)}
     *             methods.
     * 
     * @see #escape(String)
     * @see Writer
     */
    public void unescape(Writer writer, String str) throws IOException {
        int firstAmp = str.indexOf('&');
        if (firstAmp < 0) {
            writer.write(str);
            return;
        } else {
            doUnescape(writer, str, firstAmp);
        }
    }

    /**
     * Underlying unescape method that allows the optimisation of not starting from the 0 index again.
     *
     * @param writer
     *            The <code>Writer</code> to write the results to; assumed to be non-null.
     * @param str
     *            The source <code>String</code> to unescape; assumed to be non-null.
     * @param firstAmp
     *            The <code>int</code> index of the first ampersand in the source String.
     * @throws IOException
     *             when <code>Writer</code> passed throws the exception from calls to the {@link Writer#write(int)}
     *             methods.
     */
    private void doUnescape(Writer writer, String str, int firstAmp) throws IOException {
        writer.write(str, 0, firstAmp);
        int len = str.length();
        for (int i = firstAmp; i < len; i++) {
            char c = str.charAt(i);
            if (c == '&') {
                int nextIdx = i + 1;
                int semiColonIdx = str.indexOf(';', nextIdx);
                if (semiColonIdx == -1) {
                    writer.write(c);
                    continue;
                }
                int amphersandIdx = str.indexOf('&', i + 1);
                if (amphersandIdx != -1 && amphersandIdx < semiColonIdx) {
                    // Then the text looks like &...&...;
                    writer.write(c);
                    continue;
                }
                String entityContent = str.substring(nextIdx, semiColonIdx);
                int entityValue = -1;
                int entityContentLen = entityContent.length();
                if (entityContentLen > 0) {
                    if (entityContent.charAt(0) == '#') { // escaped value content is an integer (decimal or
                        // hexidecimal)
                        if (entityContentLen > 1) {
                            char isHexChar = entityContent.charAt(1);
                            try {
                                switch (isHexChar) {
                                    case 'X' :
                                    case 'x' : {
                                        entityValue = Integer.parseInt(entityContent.substring(2), 16);
                                        break;
                                    }
                                    default : {
                                        entityValue = Integer.parseInt(entityContent.substring(1), 10);
                                    }
                                }
                                if (entityValue > 0xFFFF) {
                                    entityValue = -1;
                                }
                            } catch (NumberFormatException e) {
                                entityValue = -1;
                            }
                        }
                    } else { // escaped value content is an entity name
                        entityValue = this.entityValue(entityContent);
                    }
                }

                if (entityValue == -1) {
                    writer.write('&');
                    writer.write(entityContent);
                    writer.write(';');
                } else {
                    writer.write(entityValue);
                }
                i = semiColonIdx; // move index up to the semi-colon
            } else {
                writer.write(c);
            }
        }
    }

}
