package com.genersoft.iot.vmp.sip.utils;

import com.genersoft.iot.vmp.sip.bean.SipServer;
import com.genersoft.iot.vmp.sip.bean.SipServerAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AuthorizationHeader;
import javax.sip.message.Request;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

public class DigestClientAuthenticationHelper {
    private Logger logger = LoggerFactory.getLogger(DigestClientAuthenticationHelper.class);

    public static final String DEFAULT_ALGORITHM = "MD5";
    public static final String DEFAULT_SCHEME = "Digest";

    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };


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
    private static String generateNonce() throws NoSuchAlgorithmException {
        long time = Instant.now().toEpochMilli();
        Random rand = new Random();
        long pad = rand.nextLong();
        String nonceString = Long.valueOf(time).toString()
                + Long.valueOf(pad).toString();
        byte mdbytes[] = MessageDigest.getInstance(DEFAULT_ALGORITHM).digest(nonceString.getBytes());
        return toHexString(mdbytes);
    }

    public static AuthorizationHeader getAuthorizationHeader(SipServer server, SipServerAccount account)
            throws PeerUnavailableException, ParseException, NoSuchAlgorithmException {
        return getAuthorizationHeader(server, account, null, null, null, null);
    }

    public static AuthorizationHeader getAuthorizationHeader(SipServer server, SipServerAccount account,
                                                             String realm, String nonce, String algorithm, String qop)
            throws PeerUnavailableException, ParseException, NoSuchAlgorithmException {

        SipURI requestURI = SipFactory.getInstance().createAddressFactory().createSipURI(account.getUsername(),
                server.getServerIp() + ":" + server.getServerPort());

        AuthorizationHeader authorizationHeader = SipFactory.getInstance().createHeaderFactory().createAuthorizationHeader(DEFAULT_SCHEME);
        authorizationHeader.setUsername(account.getUsername());
        if (realm == null) {
            authorizationHeader.setRealm(server.getServerIp());
        }else {
            authorizationHeader.setRealm(realm);
        }
        if (nonce == null) {
            authorizationHeader.setRealm(generateNonce());
        }else {
            authorizationHeader.setRealm(realm);
        }
        if (algorithm == null) {
            authorizationHeader.setAlgorithm("MD5");
        }else {
            authorizationHeader.setAlgorithm(algorithm);
        }
        if (qop == null) {
            authorizationHeader.setQop("auth");
        }else {
            authorizationHeader.setQop(qop);
        }

        authorizationHeader.setNonce(generateNonce());
        authorizationHeader.setNonceCount(1);
        authorizationHeader.setURI(requestURI);
        authorizationHeader.setCNonce(UUID.randomUUID().toString());

        String responseForAuth = getResponseForAuth(authorizationHeader, account.getPassword());
        authorizationHeader.setResponse(responseForAuth);
        return authorizationHeader;
    }

    private static String getResponseForAuth(AuthorizationHeader authHeader, String password) throws NoSuchAlgorithmException {

        String realm = authHeader.getRealm();
        String username = authHeader.getUsername();

        if ( username == null || realm == null ) {
            return null;
        }

        String nonce = authHeader.getNonce();
        URI uri = authHeader.getURI();
        if (uri == null) {
            return null;
        }

        String A2 =  "REGISTER:" + uri.toString();
        String HA1 = password;

        MessageDigest messageDigest = MessageDigest.getInstance(DEFAULT_ALGORITHM);

        byte[] mdbytes = messageDigest.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);

        String cnonce = authHeader.getCNonce();
        String KD = HA1 + ":" + nonce;
        if (cnonce != null) {
            KD += ":" + cnonce;
        }
        KD += ":" + HA2;
        mdbytes = messageDigest.digest(KD.getBytes());

        return toHexString(mdbytes);
    }

    /**
     * Authenticate the inbound request given plain text password.
     *
     * @param request - the request to authenticate.
     * @param pass -- the plain text password.
     *
     * @return true if authentication succeded and false otherwise.
     */
    public boolean doAuthenticatePlainTextPassword(Request request, String pass) throws NoSuchAlgorithmException {
        AuthorizationHeader authHeader = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
        if ( authHeader == null || authHeader.getRealm() == null) {
            return false;
        }
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
        String cnonce = authHeader.getCNonce();

        // nonce计数器，是一个16进制的数值，表示同一nonce下客户端发送出请求的数量
        int nc = authHeader.getNonceCount();
        String ncStr = String.format("%08x", nc).toUpperCase();
        // String ncStr = new DecimalFormat("00000000").format(nc);
        // String ncStr = new DecimalFormat("00000000").format(Integer.parseInt(nc + "", 16));

        String A1 = username + ":" + realm + ":" + pass;

        String A2 = request.getMethod().toUpperCase() + ":" + uri.toString();

        MessageDigest messageDigest = MessageDigest.getInstance(DEFAULT_ALGORITHM);

        byte mdbytes[] = messageDigest.digest(A1.getBytes());
        String HA1 = toHexString(mdbytes);
        logger.debug("A1: " + A1);
        logger.debug("A2: " + A2);
        mdbytes = messageDigest.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);
        logger.debug("HA1: " + HA1);
        logger.debug("HA2: " + HA2);
        // String cnonce = authHeader.getCNonce();
        logger.debug("nonce: " + nonce);
        logger.debug("nc: " + ncStr);
        logger.debug("cnonce: " + cnonce);
        logger.debug("qop: " + qop);
        String KD = HA1 + ":" + nonce;

        if (qop != null && qop.equalsIgnoreCase("auth") ) {
            if (nc != -1) {
                KD += ":" + ncStr;
            }
            if (cnonce != null) {
                KD += ":" + cnonce;
            }
            KD += ":" + qop;
        }
        KD += ":" + HA2;
        logger.debug("KD: " + KD);
        mdbytes = messageDigest.digest(KD.getBytes());
        String mdString = toHexString(mdbytes);
        logger.debug("mdString: " + mdString);
        String response = authHeader.getResponse();
        logger.debug("response: " + response);
        return mdString.equals(response);

    }

}
