/*
 * Conditions Of Use
 *
 * This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 Untied States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 *
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS."  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof, including but
 * not limited to the correctness, accuracy, reliability or usefulness of
 * the software.
 *
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement
 *
 * .
 *
 */
package com.genersoft.iot.vmp.gb28181.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Random;

import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.core.InternalErrorHandler;

/**
 * Implements the HTTP digest authentication method server side functionality.
 *
 * @author M. Ranganathan
 * @author Marc Bednarek
 */

public class DigestServerAuthenticationHelper  {

    private MessageDigest messageDigest;

    public static final String DEFAULT_ALGORITHM = "MD5";
    public static final String DEFAULT_SCHEME = "Digest";




    /** to hex converter */
    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Default constructor.
     * @throws NoSuchAlgorithmException
     */
    public DigestServerAuthenticationHelper()
            throws NoSuchAlgorithmException {
        messageDigest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
    }

    public static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }

    /**
     * Generate the challenge string.
     *
     * @return a generated nonce.
     */
    private String generateNonce() {
        // Get the time of day and run MD5 over it.
        Date date = new Date();
        long time = date.getTime();
        Random rand = new Random();
        long pad = rand.nextLong();
        String nonceString = (new Long(time)).toString()
                + (new Long(pad)).toString();
        byte mdbytes[] = messageDigest.digest(nonceString.getBytes());
        // Convert the mdbytes array into a hex string.
        return toHexString(mdbytes);
    }

    public Response generateChallenge(HeaderFactory headerFactory, Response response, String realm) {
        try {
            WWWAuthenticateHeader proxyAuthenticate = headerFactory
                    .createWWWAuthenticateHeader(DEFAULT_SCHEME);
            proxyAuthenticate.setParameter("realm", realm);
            proxyAuthenticate.setParameter("nonce", generateNonce());

            proxyAuthenticate.setParameter("opaque", "");
            proxyAuthenticate.setParameter("stale", "FALSE");
            proxyAuthenticate.setParameter("algorithm", DEFAULT_ALGORITHM);

//            proxyAuthenticate.setParameter("qop", "auth");
            response.setHeader(proxyAuthenticate);
        } catch (Exception ex) {
            InternalErrorHandler.handleException(ex);
        }
        return response;
    }
    /**
     * Authenticate the inbound request.
     *
     * @param request - the request to authenticate.
     * @param hashedPassword -- the MD5 hashed string of username:realm:plaintext password.
     *
     * @return true if authentication succeded and false otherwise.
     */
    public boolean doAuthenticateHashedPassword(Request request, String hashedPassword) {
        AuthorizationHeader authHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if ( authHeader == null ) return false;
        String realm = authHeader.getRealm();
        String username = authHeader.getUsername();

        if ( username == null || realm == null ) {
            return false;
        }

        String nonce = authHeader.getNonce();
        URI uri = authHeader.getURI();
        if (uri == null) {
            return false;
        }



        String A2 = request.getMethod().toUpperCase() + ":" + uri.toString();
        String HA1 = hashedPassword;


        byte[] mdbytes = messageDigest.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);

        String cnonce = authHeader.getCNonce();
        String KD = HA1 + ":" + nonce;
        if (cnonce != null) {
            KD += ":" + cnonce;
        }
        KD += ":" + HA2;
        mdbytes = messageDigest.digest(KD.getBytes());
        String mdString = toHexString(mdbytes);
        String response = authHeader.getResponse();


        return mdString.equals(response);
    }

    /**
     * Authenticate the inbound request given plain text password.
     *
     * @param request - the request to authenticate.
     * @param pass -- the plain text password.
     *
     * @return true if authentication succeded and false otherwise.
     */
    public boolean doAuthenticatePlainTextPassword(Request request, String pass) {
        AuthorizationHeader authHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if ( authHeader == null ) return false;
        String realm = authHeader.getRealm().trim();
        String username = authHeader.getUsername().trim();

        if ( username == null || realm == null ) {
            return false;
        }

        String nonce = authHeader.getNonce();
        URI uri = authHeader.getURI();
        if (uri == null) {
            return false;
        }
        // qop 保护质量 包含auth（默认的）和auth-int（增加了报文完整性检测）两种策略
        String qop = authHeader.getQop();

        // 客户端随机数，这是一个不透明的字符串值，由客户端提供，并且客户端和服务器都会使用，以避免用明文文本。
        // 这使得双方都可以查验对方的身份，并对消息的完整性提供一些保护
        String cNonce = authHeader.getCNonce();

        // nonce计数器，是一个16进制的数值，表示同一nonce下客户端发送出请求的数量
        int nc = authHeader.getNonceCount();
        String ncStr = new DecimalFormat("00000000").format(nc);
//        String ncStr = new DecimalFormat("00000000").format(Integer.parseInt(nc + "", 16));

        String A1 = username + ":" + realm + ":" + pass;
        String A2 = request.getMethod().toUpperCase() + ":" + uri.toString();
        byte mdbytes[] = messageDigest.digest(A1.getBytes());
        String HA1 = toHexString(mdbytes);
        System.out.println("A1: " + A1);
        System.out.println("A2: " + A2);

        mdbytes = messageDigest.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);
        System.out.println("HA1: " + HA1);
        System.out.println("HA2: " + HA2);
        String cnonce = authHeader.getCNonce();
        System.out.println("nonce: " + nonce);
        System.out.println("nc: " + ncStr);
        System.out.println("cnonce: " + cnonce);
        System.out.println("qop: " + qop);
        String KD = HA1 + ":" + nonce;

        if (qop != null && qop.equals("auth") ) {
            if (nc != -1) {
                KD += ":" + ncStr;
            }
            if (cnonce != null) {
                KD += ":" + cnonce;
            }
            KD += ":" + qop;
        }
        KD += ":" + HA2;
        System.out.println("KD: " + KD);
        mdbytes = messageDigest.digest(KD.getBytes());
        String mdString = toHexString(mdbytes);
        System.out.println("mdString: " + mdString);
        String response = authHeader.getResponse();
        System.out.println("response: " + response);
        return mdString.equals(response);

    }
}
