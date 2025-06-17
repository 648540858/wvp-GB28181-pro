package com.genersoft.iot.vmp.gat1400.backend.security;

import com.genersoft.iot.vmp.gat1400.utils.Digests;

import org.apache.commons.codec.binary.Hex;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.Data;

@Data
public class DigestData {
    public static final String DIGEST_KEY = "foshan";
    public static final String DIGEST_REALM = "viid";
    protected static MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    /**
     * 第一次注册请求带过来 realm qop nonce opaque
     */
    private final String realm;
    private final String qop;
    private final String nonce;
    private final String opaque;

    private String username;
    private String uri;
    private String nc;
    private String response;
    private String cnonce;
    private final String section212response;
    private long nonceExpiryTime;

    public DigestData(String header) {
        this.section212response = header.substring(7);
        String[] headerEntries = DigestAuthUtils.splitIgnoringQuotes(this.section212response, ',');
        Map<String, String> headerMap = DigestAuthUtils.splitEachArrayElementAndCreateMap(headerEntries, "=", "\"");
        this.username = headerMap.getOrDefault("username", "null");
        this.realm = headerMap.getOrDefault("realm", "null");
        this.nonce = headerMap.getOrDefault("nonce", "null");
        this.response = headerMap.getOrDefault("response", null);
        this.qop = headerMap.getOrDefault("qop", "null");
        this.uri = Optional.ofNullable(headerMap.get("uri")).orElse("/VIID/System/Register");
        this.nc = Optional.ofNullable(headerMap.get("nc")).orElse("00000001");
        this.cnonce = headerMap.get("cnonce");
        if (Objects.isNull(this.cnonce)) {
            this.cnonce = new String(Hex.encodeHex(Digests.generateSalt(8)));
        }
        this.opaque = headerMap.getOrDefault("opaque", "null");
    }

    /**
     * 转换第一次注册请求带回来的摘要数据并写入第二次请求摘要信息
     * @param username 用户名
     * @param password 凭证
     * @param httpMethod http方法
     * @return 第二次请求 authenticate 字符串
     */
    public String toDigestHeader(String username, String password, String httpMethod) {
        this.username = username;
        if (Objects.isNull(this.response)) {
            this.response = this.calculateServerDigest(password, httpMethod);
        }
        return String.format("Digest username=\"%s\", realm=\"%s\", nonce=\"%s\", uri=\"%s\", response=\"%s\", qop=\"%s\", nc=\"%s\", cnonce=\"%s\", algorithm=\"%s\", opaque=\"%s\"",
                this.username, this.realm, this.nonce, this.uri, this.response, this.qop, this.nc, this.cnonce, "MD5", this.opaque);
    }

    public String calculateServerDigest(String password, String httpMethod) {
        return DigestAuthUtils.generateDigest(false, this.username, this.realm, password, httpMethod, this.uri, this.qop, this.nonce, this.nc, this.cnonce);
    }

    public void validateAndDecode(String entryPointKey, String expectedRealm) throws BadCredentialsException {
        if (this.username != null && this.realm != null && this.nonce != null && this.uri != null && this.response != null) {
            if ("auth".equals(this.qop) && (this.nc == null || this.cnonce == null)) {
                throw new BadCredentialsException(messages.getMessage("DigestAuthenticationFilter.missingAuth", new Object[]{this.section212response}, "Missing mandatory digest value; received header {0}"));
            } else if (!expectedRealm.equals(this.realm)) {
                throw new BadCredentialsException(messages.getMessage("DigestAuthenticationFilter.incorrectRealm", new Object[]{this.realm, expectedRealm}, "Response realm name '{0}' does not match system realm name of '{1}'"));
            } else {
                try {
                    Base64.getDecoder().decode(this.nonce.getBytes());
                } catch (IllegalArgumentException var7) {
                    throw new BadCredentialsException(messages.getMessage("DigestAuthenticationFilter.nonceEncoding", new Object[]{this.nonce}, "Nonce is not encoded in Base64; received nonce {0}"));
                }

                String nonceAsPlainText = new String(Base64.getDecoder().decode(this.nonce.getBytes()));
                String[] nonceTokens = StringUtils.delimitedListToStringArray(nonceAsPlainText, ":");
                if (nonceTokens.length != 2) {
                    throw new BadCredentialsException(messages.getMessage("DigestAuthenticationFilter.nonceNotTwoTokens", new Object[]{nonceAsPlainText}, "Nonce should have yielded two tokens but was {0}"));
                } else {
                    try {
                        this.nonceExpiryTime = new Long(nonceTokens[0]);
                    } catch (NumberFormatException var6) {
                        throw new BadCredentialsException(messages.getMessage("DigestAuthenticationFilter.nonceNotNumeric", new Object[]{nonceAsPlainText}, "Nonce token should have yielded a numeric first token, but was {0}"));
                    }

                    String expectedNonceSignature = DigestAuthUtils.md5Hex(this.nonceExpiryTime + ":" + entryPointKey);
                    if (!expectedNonceSignature.equals(nonceTokens[1])) {
                        throw new BadCredentialsException(messages.getMessage("DigestAuthenticationFilter.nonceCompromised", new Object[]{nonceAsPlainText}, "Nonce token compromised {0}"));
                    }
                }
            }
        } else {
            throw new BadCredentialsException(messages.getMessage("DigestAuthenticationFilter.missingMandatory", new Object[]{this.section212response}, "Missing mandatory digest value; received header {0}"));
        }
    }
}
