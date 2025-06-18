package com.genersoft.iot.vmp.gat1400.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.Validate;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;

/**
 * <p>
 * RFC2671加密
 * </p>
 *
 **/
public class Digests {

    private static final SecureRandom random = new SecureRandom();
    private static final String delimiter = ":";

    /**
     * 加密遵循RFC2671规范 将相关参数加密生成一个MD5字符串,并返回
     */
    public static String http_da_calc_HA1(String username, String realm, String password,
                                          String nonce, String nc, String cnonce, String qop,
                                          String method, String uri, String algorithm) {
        String HA1, HA2;
        if ("MD5-sess".equals(algorithm)) {
            HA1 = HA1_MD5_sess(username, realm, password, nonce, cnonce);
        } else {
            HA1 = HA1_MD5(username, realm, password);
        }
        byte[] md5Byte = md5(HA1.getBytes());
        HA1 = new String(Hex.encodeHex(md5Byte));


        md5Byte = md5(HA2(method, uri).getBytes());
        HA2 = new String(Hex.encodeHex(md5Byte));

        String original = HA1 + ":" + (nonce + ":" + nc + ":" + cnonce + ":" + qop) + ":" + HA2;
//        String original = StringUtils.join(HA1, delimiter,
//                StringUtils.join(nonce, delimiter, nc, delimiter, cnonce, delimiter, qop),
//                delimiter, HA2);

        md5Byte = md5(original.getBytes());
        return new String(Hex.encodeHex(md5Byte));
    }

    /**
     * algorithm值为MD5时规则
     */
    private static String HA1_MD5(String username, String realm, String password) {
//        return StringUtils.join(username, delimiter, realm, delimiter, password);
        return username + ":" + realm + ":" + password;
    }

    /**
     * algorithm值为MD5-sess时规则
     */
    private static String HA1_MD5_sess(String username, String realm, String password, String nonce, String cnonce) {
        //      MD5(username:realm:password):nonce:cnonce

        String s = HA1_MD5(username, realm, password);
        byte[] md5Byte = md5(s.getBytes());
        String smd5 = new String(Hex.encodeHex(md5Byte));

//        StringUtils.join(smd5, delimiter, nonce, delimiter, cnonce);
        return smd5 + ":" + nonce + ":" + cnonce;
    }

    private static String HA2(String method, String uri) {
//        StringUtils.join(method, delimiter, uri)
        return method + ":" + uri;
    }

    /**
     * 对输入字符串进行md5散列.
     */
    public static byte[] md5(byte[] input) {
        return digest(input, "MD5", null, 1);
    }

    /**
     * 对字符串进行散列, 支持md5与sha1算法.
     */
    private static byte[] digest(byte[] input, String algorithm, byte[] salt, int iterations) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            if (salt != null) {
                digest.update(salt);
            }

            byte[] result = digest.digest(input);

            for (int i = 1; i < iterations; i++) {
                digest.reset();
                result = digest.digest(result);
            }
            return result;
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 随机生成numBytes长度数组
     *
     * @param numBytes
     * @return
     */
    public static byte[] generateSalt(int numBytes) {
        Validate.isTrue(numBytes > 0, "numBytes argument must be a positive integer (1 or larger)", (long) numBytes);
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);
        return bytes;
    }

    @Deprecated
    public static String generateSalt2(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        //参数length，表示生成几位随机数
        for (int i = 0; i < length; i++) {
            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
            //输出字母还是数字
            if ("char".equalsIgnoreCase(charOrNum)) {
                //输出是大写字母还是小写字母
                int temp = random.nextInt(2) % 2 == 0 ? 65 : 97;
                sb.append((char) (random.nextInt(26) + temp));
            } else {
                sb.append(String.valueOf(random.nextInt(10)));
            }
        }
        return sb.toString().toLowerCase();
    }

}
