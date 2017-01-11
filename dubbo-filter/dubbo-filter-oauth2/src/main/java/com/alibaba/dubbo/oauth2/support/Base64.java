//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.alibaba.dubbo.oauth2.support;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class Base64 {
    public static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final int[] INV = new int[256];

    public Base64() {
    }

    public static char[] encodeToChar(byte[] arr, boolean lineSeparator) {
        int len = arr != null?arr.length:0;
        if(len == 0) {
            return new char[0];
        } else {
            int evenlen = len / 3 * 3;
            int cnt = (len - 1) / 3 + 1 << 2;
            int destLen = cnt + (lineSeparator?(cnt - 1) / 76 << 1:0);
            char[] dest = new char[destLen];
            int left = 0;
            int i = 0;
            int cc = 0;

            while(left < evenlen) {
                int i1 = (arr[left++] & 255) << 16 | (arr[left++] & 255) << 8 | arr[left++] & 255;
                dest[i++] = CHARS[i1 >>> 18 & 63];
                dest[i++] = CHARS[i1 >>> 12 & 63];
                dest[i++] = CHARS[i1 >>> 6 & 63];
                dest[i++] = CHARS[i1 & 63];
                if(lineSeparator) {
                    ++cc;
                    if(cc == 19 && i < destLen - 2) {
                        dest[i++] = 13;
                        dest[i++] = 10;
                        cc = 0;
                    }
                }
            }

            left = len - evenlen;
            if(left > 0) {
                i = (arr[evenlen] & 255) << 10 | (left == 2?(arr[len - 1] & 255) << 2:0);
                dest[destLen - 4] = CHARS[i >> 12];
                dest[destLen - 3] = CHARS[i >>> 6 & 63];
                dest[destLen - 2] = left == 2?CHARS[i & 63]:61;
                dest[destLen - 1] = 61;
            }

            return dest;
        }
    }

    public byte[] decode(char[] arr) {
        int length = arr.length;
        if(length == 0) {
            return new byte[0];
        } else {
            int sndx = 0;
            int endx = length - 1;
            int pad = arr[endx] == 61?(arr[endx - 1] == 61?2:1):0;
            int cnt = endx - sndx + 1;
            int sepCnt = length > 76?(arr[76] == 13?cnt / 78:0) << 1:0;
            int len = ((cnt - sepCnt) * 6 >> 3) - pad;
            byte[] dest = new byte[len];
            int d = 0;
            int i = 0;
            int r = len / 3 * 3;

            while(d < r) {
                int i1 = INV[arr[sndx++]] << 18 | INV[arr[sndx++]] << 12 | INV[arr[sndx++]] << 6 | INV[arr[sndx++]];
                dest[d++] = (byte)(i1 >> 16);
                dest[d++] = (byte)(i1 >> 8);
                dest[d++] = (byte)i1;
                if(sepCnt > 0) {
                    ++i;
                    if(i == 19) {
                        sndx += 2;
                        i = 0;
                    }
                }
            }

            if(d < len) {
                i = 0;

                for(r = 0; sndx <= endx - pad; ++r) {
                    i |= INV[arr[sndx++]] << 18 - r * 6;
                }

                for(r = 16; d < len; r -= 8) {
                    dest[d++] = (byte)(i >> r);
                }
            }

            return dest;
        }
    }

    public static byte[] encodeToByte(String s) {
        try {
            return encodeToByte(s.getBytes("UTF-8"), false);
        } catch (UnsupportedEncodingException var2) {
            return null;
        }
    }

    public static byte[] encodeToByte(String s, boolean lineSep) {
        try {
            return encodeToByte(s.getBytes("UTF-8"), lineSep);
        } catch (UnsupportedEncodingException var3) {
            return null;
        }
    }

    public static byte[] encodeToByte(byte[] arr) {
        return encodeToByte(arr, false);
    }

    public static byte[] encodeToByte(byte[] arr, boolean lineSep) {
        int len = arr != null?arr.length:0;
        if(len == 0) {
            return new byte[0];
        } else {
            int evenlen = len / 3 * 3;
            int cnt = (len - 1) / 3 + 1 << 2;
            int destlen = cnt + (lineSep?(cnt - 1) / 76 << 1:0);
            byte[] dest = new byte[destlen];
            int left = 0;
            int i = 0;
            int cc = 0;

            while(left < evenlen) {
                int i1 = (arr[left++] & 255) << 16 | (arr[left++] & 255) << 8 | arr[left++] & 255;
                dest[i++] = (byte)CHARS[i1 >>> 18 & 63];
                dest[i++] = (byte)CHARS[i1 >>> 12 & 63];
                dest[i++] = (byte)CHARS[i1 >>> 6 & 63];
                dest[i++] = (byte)CHARS[i1 & 63];
                if(lineSep) {
                    ++cc;
                    if(cc == 19 && i < destlen - 2) {
                        dest[i++] = 13;
                        dest[i++] = 10;
                        cc = 0;
                    }
                }
            }

            left = len - evenlen;
            if(left > 0) {
                i = (arr[evenlen] & 255) << 10 | (left == 2?(arr[len - 1] & 255) << 2:0);
                dest[destlen - 4] = (byte)CHARS[i >> 12];
                dest[destlen - 3] = (byte)CHARS[i >>> 6 & 63];
                dest[destlen - 2] = left == 2?(byte)CHARS[i & 63]:61;
                dest[destlen - 1] = 61;
            }

            return dest;
        }
    }

    public static String decodeToString(byte[] arr) {
        try {
            return new String(decode(arr), "UTF-8");
        } catch (UnsupportedEncodingException var2) {
            return null;
        }
    }

    public static byte[] decode(byte[] arr) {
        int length = arr.length;
        if(length == 0) {
            return new byte[0];
        } else {
            int sndx = 0;
            int endx = length - 1;
            int pad = arr[endx] == 61?(arr[endx - 1] == 61?2:1):0;
            int cnt = endx - sndx + 1;
            int sepCnt = length > 76?(arr[76] == 13?cnt / 78:0) << 1:0;
            int len = ((cnt - sepCnt) * 6 >> 3) - pad;
            byte[] dest = new byte[len];
            int d = 0;
            int i = 0;
            int r = len / 3 * 3;

            while(d < r) {
                int i1 = INV[arr[sndx++]] << 18 | INV[arr[sndx++]] << 12 | INV[arr[sndx++]] << 6 | INV[arr[sndx++]];
                dest[d++] = (byte)(i1 >> 16);
                dest[d++] = (byte)(i1 >> 8);
                dest[d++] = (byte)i1;
                if(sepCnt > 0) {
                    ++i;
                    if(i == 19) {
                        sndx += 2;
                        i = 0;
                    }
                }
            }

            if(d < len) {
                i = 0;

                for(r = 0; sndx <= endx - pad; ++r) {
                    i |= INV[arr[sndx++]] << 18 - r * 6;
                }

                for(r = 16; d < len; r -= 8) {
                    dest[d++] = (byte)(i >> r);
                }
            }

            return dest;
        }
    }

    public static String encodeToString(String s) {
        try {
            return new String(encodeToChar(s.getBytes("UTF-8"), false));
        } catch (UnsupportedEncodingException var2) {
            return null;
        }
    }

    public static String encodeToString(String s, boolean lineSep) {
        try {
            return new String(encodeToChar(s.getBytes("UTF-8"), lineSep));
        } catch (UnsupportedEncodingException var3) {
            return null;
        }
    }

    public static String encodeToString(byte[] arr) {
        return new String(encodeToChar(arr, false));
    }

    public static String encodeToString(byte[] arr, boolean lineSep) {
        return new String(encodeToChar(arr, lineSep));
    }

    public static String decodeToString(String s) {
        try {
            return new String(decode(s), "UTF-8");
        } catch (UnsupportedEncodingException var2) {
            return null;
        }
    }

    public static byte[] decode(String s) {
        int length = s.length();
        if(length == 0) {
            return new byte[0];
        } else {
            int sndx = 0;
            int endx = length - 1;
            int pad = s.charAt(endx) == 61?(s.charAt(endx - 1) == 61?2:1):0;
            int cnt = endx - sndx + 1;
            int sepCnt = length > 76?(s.charAt(76) == 13?cnt / 78:0) << 1:0;
            int len = ((cnt - sepCnt) * 6 >> 3) - pad;
            byte[] dest = new byte[len];
            int d = 0;
            int i = 0;
            int r = len / 3 * 3;

            while(d < r) {
                int i1 = INV[s.charAt(sndx++)] << 18 | INV[s.charAt(sndx++)] << 12 | INV[s.charAt(sndx++)] << 6 | INV[s.charAt(sndx++)];
                dest[d++] = (byte)(i1 >> 16);
                dest[d++] = (byte)(i1 >> 8);
                dest[d++] = (byte)i1;
                if(sepCnt > 0) {
                    ++i;
                    if(i == 19) {
                        sndx += 2;
                        i = 0;
                    }
                }
            }

            if(d < len) {
                i = 0;

                for(r = 0; sndx <= endx - pad; ++r) {
                    i |= INV[s.charAt(sndx++)] << 18 - r * 6;
                }

                for(r = 16; d < len; r -= 8) {
                    dest[d++] = (byte)(i >> r);
                }
            }

            return dest;
        }
    }

    static {
        Arrays.fill(INV, -1);
        int i = 0;

        for(int iS = CHARS.length; i < iS; INV[CHARS[i]] = i++) {
            ;
        }

        INV[61] = 0;
    }
}
