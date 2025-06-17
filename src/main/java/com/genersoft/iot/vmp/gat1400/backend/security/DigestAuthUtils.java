package com.genersoft.iot.vmp.gat1400.backend.security;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DigestAuthUtils {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private DigestAuthUtils() {
    }

    public static String encodePasswordInA1Format(String username, String realm, String password) {
        String a1 = username + ":" + realm + ":" + password;
        return md5Hex(a1);
    }

    public static String[] splitIgnoringQuotes(String str, char separatorChar) {
        if (str == null) {
            return null;
        } else {
            int len = str.length();
            if (len == 0) {
                return EMPTY_STRING_ARRAY;
            } else {
                List<String> list = new ArrayList<>();
                int i = 0;
                int start = 0;
                boolean match = false;

                while(true) {
                    while(i < len) {
                        if (str.charAt(i) == '"') {
                            ++i;

                            while(i < len) {
                                if (str.charAt(i) == '"') {
                                    ++i;
                                    break;
                                }

                                ++i;
                            }

                            match = true;
                        } else if (str.charAt(i) == separatorChar) {
                            if (match) {
                                list.add(str.substring(start, i));
                                match = false;
                            }

                            ++i;
                            start = i;
                        } else {
                            match = true;
                            ++i;
                        }
                    }

                    if (match) {
                        list.add(str.substring(start, i));
                    }

                    return list.toArray(new String[0]);
                }
            }
        }
    }

    public static String generateDigest(boolean passwordAlreadyEncoded, String username, String realm, String password, String httpMethod, String uri, String qop, String nonce, String nc, String cnonce) throws IllegalArgumentException {
        String a2 = httpMethod + ":" + uri;
        String a1Md5 = !passwordAlreadyEncoded ? encodePasswordInA1Format(username, realm, password) : password;
        String a2Md5 = md5Hex(a2);
        if (qop == null) {
            return md5Hex(a1Md5 + ":" + nonce + ":" + a2Md5);
        } else if ("auth".equals(qop)) {
            return md5Hex(a1Md5 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + a2Md5);
        } else {
            throw new IllegalArgumentException("This method does not support a qop: '" + qop + "'");
        }
    }

    public static Map<String, String> splitEachArrayElementAndCreateMap(String[] array, String delimiter, String removeCharacters) {
        if (array != null && array.length != 0) {
            Map<String, String> map = new HashMap<>();
            int var5 = array.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String s = array[var6];
                String postRemove = removeCharacters != null ? StringUtils.replace(s, removeCharacters, "") : s;
                String[] splitThisArrayElement = split(postRemove, delimiter);
                if (splitThisArrayElement != null) {
                    map.put(splitThisArrayElement[0].trim(), splitThisArrayElement[1].trim());
                }
            }

            return map;
        } else {
            return null;
        }
    }

    public static String[] split(String toSplit, String delimiter) {
        Assert.hasLength(toSplit, "Cannot split a null or empty string");
        Assert.hasLength(delimiter, "Cannot use a null or empty delimiter to split a string");
        Assert.isTrue(delimiter.length() == 1, "Delimiter can only be one character in length");
        int offset = toSplit.indexOf(delimiter);
        if (offset < 0) {
            return null;
        } else {
            String beforeDelimiter = toSplit.substring(0, offset);
            String afterDelimiter = toSplit.substring(offset + 1);
            return new String[]{beforeDelimiter, afterDelimiter};
        }
    }

    public static String md5Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return new String(Hex.encode(digest.digest(data.getBytes())));
        } catch (NoSuchAlgorithmException var2) {
            throw new IllegalStateException("No MD5 algorithm available!");
        }
    }
}
