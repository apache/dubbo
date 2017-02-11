package com.alibaba.dubbo.rpc.protocol.redis2;

import java.security.MessageDigest;

/**
 * Created by wuyu on 2017/2/11.
 */
public class Md5Util {

    public static byte[] md5(byte[] md5) {
        try {
            return MessageDigest.getInstance("MD5").digest(md5);
        } catch (Exception e) {

        }
        return md5;
    }

    public static String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte b[] = md.digest();

            int i;

            StringBuilder buf = new StringBuilder("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            str = buf.toString();
        } catch (Exception e) {
            e.printStackTrace();

        }
        return str;
    }


    public static String getSortMD5(String str) {

        String[] chars = new String[]{"a", "b", "c", "d", "e", "f", "g", "h",
                "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t",
                "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
                "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H",
                "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
                "U", "V", "W", "X", "Y", "Z"
        };
        String hex = md5(str);

        String sTempSubString = hex.substring(0, 8);

        long lHexLong = 0x3FFFFFFF & Long.parseLong(sTempSubString, 16);

        String outChars = "";

        for (int j = 0; j < 6; j++) {
            long index = 0x0000003D & lHexLong;
            outChars += chars[(int) index];
            lHexLong = lHexLong >> 5;
        }

        return outChars;
    }
}
